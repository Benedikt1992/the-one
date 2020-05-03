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

    private Set<ContactPlanEdge> visitedEdges;
    protected Map<Integer, ContactPlanNode> nodesByAddress;

    protected ContactPlanGraph(Settings contactSettings) {
        super();
        this.visitedEdges = new HashSet<>();
        nodesByAddress = new HashMap<>();
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

//        int i = 0;
//        int max = destination.getSizeInEdges();
        for (Iterator<ContactPlanEdge> it = destination.incomingEdges(false); it.hasNext(); ) {
//            i++;
            ContactPlanEdge edge = it.next();
//            System.out.println(i + "/" + max);
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
        if (carriers.contains(edge.getTo().getAddress())) {
            return;
        }
        if (routeState.size() > 10) {
            // TODO introduce route length limit with disable option?
            return;
        }
        visitedEdges.add(edge);
        routeState.push(new Tuple<>(edge.getEnd(), edge.getTo().getAddress()));
        carriers.add(edge.getTo().getAddress());
        ContactPlanNode node = edge.getFrom();
        LinkedList<Tuple<Double, Integer>> clone = ( LinkedList<Tuple<Double, Integer>>) routeState.clone();
        node.setRouteCandidate(clone);
        double end = edge.getEnd();
        for (Tuple<Double, Integer> entry: clone) {
            // It can happen that a previous hop as alater end time than the successor.
            if (entry.getKey() < end) {
                end = entry.getKey();
            }
        }
        Set<ContactPlanEdge> contacts = node.getPreviousContacts(edge, end);
        for (ContactPlanEdge contact : contacts) {
//            if (contact.getTo().getAddress().equals(473) || contact.getFrom().getAddress().equals(473)) {
//                System.out.println("Examine");
//            }
            deepSearch(contact, routeState, carriers);
        }
        routeState.pop();
        carriers.remove(edge.getTo().getAddress());
    }
}
