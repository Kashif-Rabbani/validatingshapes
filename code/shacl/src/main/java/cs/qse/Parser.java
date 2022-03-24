package cs.qse;

import cs.Main;
import cs.utils.*;
import cs.utils.Encoder;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class parses RDF NT file triples to extract SHACL shapes and compute the entity count for shape constraints
 */
public class Parser {
    String rdfFilePath;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    Encoder encoder;
    StatsComputer statsComputer;
    String typePredicate;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates
    
    Map<Node, EntityData> entityDataHashMap; // Size == N For every entity we save a number of summary information 
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, EntityCount> shapeTripletStats; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their entity count
    
    public Parser(String filePath, int expNoOfClasses, int expNoOfInstances, String typePredicate) {
        this.rdfFilePath = filePath;
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataHashMap = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        this.encoder = new Encoder();
    }
    
    public void run() {
        runParser();
    }
    
    private void runParser() {
        firstPass();
        secondPass();
        computeStatistics();
        extractSHACLShapes();
    }
    
    /**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to extract set of entity types and frequency of each entity.
     */
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).forEach(line -> {
                try {
                    // Get [S,P,O] as Node from triple
                    Node[] nodes = NxParser.parseNodes(line); // how much time is spent parsing?
                    if (nodes[1].toString().equals(typePredicate)) { // Check if predicate is rdf:type or equivalent
                        // Track classes per entity
                        int objID = encoder.encode(nodes[2].getLabel());
                        EntityData entityData = entityDataHashMap.get(nodes[0]);
                        if (entityData == null) {
                            entityData = new EntityData();
                        }
                        entityData.getClassTypes().add(objID);
                        entityDataHashMap.put(nodes[0], entityData);
                        classEntityCount.merge(objID, 1, Integer::sum);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * Streaming over RDF (NT Format) triples <s,p,o> line by line to collect the constraints and the metadata required
     */
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFilePath)).filter(line -> !line.contains(typePredicate)).forEach(line -> {
                try {
                    //Declaring required sets
                    Set<Integer> objTypes = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    Node[] nodes = NxParser.parseNodes(line); // parsing <s,p,o> of triple from each line as node[0], node[1], and node[2]
                    Node subject = nodes[0];
                    String objectType = extractObjectType(nodes[2].toString());
                    int propID = encoder.encode(nodes[1].getLabel());
                    if (objectType.equals("IRI")) { // object is an instance or entity of some class e.g., :Paris is an instance of :City & :Capital
                        EntityData currEntityData = entityDataHashMap.get(nodes[2]);
                        if (currEntityData != null) {
                            objTypes = currEntityData.getClassTypes();
                            for (Integer node : objTypes) { // get classes of node2
                                prop2objTypeTuples.add(new Tuple2<>(propID, node));
                            }
                            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                        }
                        /*else { // If we do not have data this is an unlabelled IRI objTypes = Collections.emptySet(); }*/
                        
                    } else { // Object is of type literal, e.g., xsd:String, xsd:Integer, etc.
                        int objID = encoder.encode(objectType);
                        //objTypes = Collections.singleton(objID); Removed because the set throws an UnsupportedOperationException if modification operation (add) is performed on it later in the loop
                        objTypes.add(objID);
                        prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
                        addEntityToPropertyConstraints(prop2objTypeTuples, subject);
                    }
                    
                    EntityData entityData = entityDataHashMap.get(subject);
                    if (entityData != null) {
                        for (Integer entityClass : entityData.getClassTypes()) {
                            Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.get(entityClass);
                            if (propToObjTypes == null) {
                                propToObjTypes = new HashMap<>();
                                classToPropWithObjTypes.put(entityClass, propToObjTypes);
                            }
                            
                            Set<Integer> classObjTypes = propToObjTypes.get(propID);
                            if (classObjTypes == null) {
                                classObjTypes = new HashSet<>();
                                propToObjTypes.put(propID, classObjTypes);
                            }
                            
                            classObjTypes.addAll(objTypes);
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("secondPass", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    /**
     * A utility method to add property constraints of each entity in the 2nd pass
     *
     * @param prop2objTypeTuples : Tuples containing property and its object type, e.g., Tuple2<livesIn, :City>, Tuple2<livesIn, :Capital>
     * @param subject            : Subject entity such as :Paris
     */
    private void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Node subject) {
        EntityData currentEntityData = entityDataHashMap.get(subject);
        if (currentEntityData == null) {
            currentEntityData = new EntityData();
        }
        //Add Property Constraint and Property cardinality
        for (Tuple2<Integer, Integer> tuple2 : prop2objTypeTuples) {
            currentEntityData.addPropertyConstraint(tuple2._1, tuple2._2);
            if (Main.extractMaxCardConstraints) {
                currentEntityData.addPropertyCardinality(tuple2._1);
            }
        }
        //Add entity data into the map
        entityDataHashMap.put(subject, currentEntityData);
    }
    
    /**
     * A utility method to extract the literal object type
     *
     * @param literalIri : IRI for the literal object
     * @return String literal type : for example RDF.LANGSTRING, XSD.STRING, XSD.INTEGER, XSD.DATE, etc.
     */
    private String extractObjectType(String literalIri) {
        Literal theLiteral = new Literal(literalIri, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {   // is literal type
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {  // is rdf:lang type
            type = "<" + RDF.LANGSTRING + ">"; //theLiteral.getLanguageTag(); will return the language tag
        } else {
            if (Utils.isValidIRI(literalIri)) {
                if (SimpleValueFactory.getInstance().createIRI(literalIri).isIRI())
                    type = "IRI";
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    /**
     * Computing statistics using the metadata extracted in the 2nd pass for shape constraints
     */
    public void computeStatistics() {
        StopWatch watch = new StopWatch();
        watch.start();
        shapeTripletStats = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        statsComputer = new StatsComputer();
        statsComputer.setShapeTripletStats(shapeTripletStats);
        statsComputer.computeStatistics(entityDataHashMap, classEntityCount);
        watch.stop();
        Utils.logTime("computeStatistics", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        
    }
    
    /**
     * Extracting shapes in SHACL syntax
     */
    private void extractSHACLShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        ShapesExtractor se = new ShapesExtractor(encoder, shapeTripletStats, classEntityCount);
        se.initiateShapesConstruction(classToPropWithObjTypes);
        watch.stop();
        Utils.logTime("extractSHACLShapes", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}