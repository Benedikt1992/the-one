package routing.contactgraph;

import core.Settings;
import core.SettingsError;
import input.Contact;
import input.ContactPlanReader;
import movement.map.MapNode;
import util.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ContactPlanGraph extends ContactGraph{

    public static final String CONTACT_PLAN = "contactPlan";
    public static final String HOP_LIMIT = "hopLimit";

    private Set<ContactPlanEdge> visitedEdges;
    private Integer hopLimit;
    protected Map<Integer, ContactPlanNode> nodesByAddress;


    protected ContactPlanGraph(Settings contactSettings) {
        super();
        this.visitedEdges = new HashSet<>();
        nodesByAddress = new HashMap<>();
        this.hopLimit = contactSettings.getInt(HOP_LIMIT);
        initializeGraph(contactSettings.getSetting(CONTACT_PLAN));

    }

    private void initializeGraph(String fileName) {
        ContactPlanReader reader = new ContactPlanReader();
        File contactFile = null;
        Map<Integer, List<Contact>> contactPlan;

        try {
            contactFile = new File(fileName);
            contactPlan = reader.readContacts(contactFile);
        }
        catch (IOException ioe){
            throw new SettingsError("Couldn't read MapRoute-data file " +
                    fileName + 	" (cause: " + ioe.getMessage() + ")");
        }

        for (Map.Entry<Integer, List<Contact>> entry: contactPlan.entrySet()) {
            ContactPlanNode host = nodesByAddress.getOrDefault(entry.getKey(), new ContactPlanNode(entry.getKey()));
            nodesByAddress.put(entry.getKey(), host);
            for (Contact contact: entry.getValue()) {
                ContactPlanNode contactNode = nodesByAddress.getOrDefault(contact.getPartner(), new ContactPlanNode(contact.getPartner()));
                nodesByAddress.put(contact.getPartner(), contactNode);
                ContactPlanEdge newEdge = new ContactPlanEdge(host, contactNode, contact.getStart(), contact.getEnd());
                addEdge(newEdge);
            }
        }
    }

    @Override
    public void addNode(Integer address) {}

    @Override
    public void addNode(Integer address, MapNode location) {}

    @Override
    protected ContactGraphNode getNode(Integer address) {
        return nodesByAddress.getOrDefault(address, null);
    }

    private void addEdge(ContactPlanEdge edge) {
        ContactPlanNode from = edge.getFrom();
        ContactPlanNode to = edge.getTo();

        from.addOutgoingEdge(edge);
        to.addIncomingEdge(edge);
    }

    public void calculateRoutesTo(Integer address) {
        if (availableRoutes.contains(address)) {
            return;
        }
        ContactPlanNode destination = this.nodesByAddress.getOrDefault(address, null);
        if (destination == null) {
            throw new RuntimeException("Requested destination for routes is not part of the contact graph.");
        }

        for (Iterator<ContactPlanEdge> it = destination.incomingEdges(false); it.hasNext(); ) {
            ContactPlanEdge edge = it.next();
            deepSearch(edge, new LinkedList<>(), new HashSet<>());
            finalizeRoutes(address);
        }

        availableRoutes.add(address);
    }

    private void finalizeRoutes(Integer destination) {
        for (Map.Entry<Integer, ContactPlanNode> entry :
                nodesByAddress.entrySet()) {
            entry.getValue().persistRouteCandidate(destination);
        }
        this.visitedEdges = new HashSet<>();
    }

    private void deepSearch(ContactPlanEdge edge, LinkedList<Tuple<Double, Integer>> routeState, Set<Integer> carriers) {
        if (visitedEdges.contains(edge)) { return; }
        visitedEdges.add(edge);
        routeState.push(new Tuple<>(edge.getEnd(), edge.getTo().getAddress()));
        carriers.add(edge.getTo().getAddress());
        ContactPlanNode node = edge.getFrom();
        LinkedList<Tuple<Double, Integer>> clone = ( LinkedList<Tuple<Double, Integer>>) routeState.clone();
        node.setRouteCandidate(clone);
        if (!carriers.contains(edge.getFrom().getAddress()) && routeState.size() < this.hopLimit) {
            List<ContactPlanEdge> contacts = node.getPreviousContacts(edge, edge.getStart());
            for (ContactPlanEdge contact : contacts) {
                deepSearch(contact, routeState, carriers);
            }
        }
        routeState.pop();
        carriers.remove(edge.getTo().getAddress());
    }
}
