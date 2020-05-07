package routing.contactgraph;

import java.util.*;

public class ContactPlanNode  extends ContactGraphNode {
    private Integer address;
    private List<ContactPlanEdge> incomingEdges;
    private List<ContactPlanEdge> outgoingEdges;
    private boolean incomingSorted;
    private boolean outgoingSorted;

    public ContactPlanNode(Integer address) {
        super();
        this.address = address;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.incomingSorted = false;
        this.outgoingSorted = false;
    }

    public Integer getAddress() {
        return address;
    }

    public void addOutgoingEdge(ContactPlanEdge edge) {
        outgoingEdges.add(edge);
        outgoingSorted = false;
    }

    public void addIncomingEdge(ContactPlanEdge edge) {
        incomingEdges.add(edge);
        incomingSorted = false;
    }

    private void sortIncomingEdges() {
        if (!incomingSorted) {
            Collections.sort(incomingEdges, Comparator.comparingDouble(ContactPlanEdge::getStart));
            incomingSorted = true;
        }
    }

    private void sortOutgoingEdges() {
        if (!outgoingSorted) {
            Collections.sort(incomingEdges, (e1, e2) -> (int) Math.floor(e1.getStart() - e2.getStart()));
            outgoingSorted = true;
        }
    }

    public Iterator<ContactPlanEdge> incomingEdges(boolean ascending) {
        sortIncomingEdges();
        if (ascending) {
            return new AscendingEdgeIterator<>(this.incomingEdges);
        } else {
            return new DescendingEdgeIterator<>(this.incomingEdges);
        }
    }

    public Set<ContactPlanEdge> getPreviousContacts(ContactPlanEdge edge, Double end) {
        Set<ContactPlanEdge> contacts = new HashSet<>();
        sortIncomingEdges();
        for (ContactPlanEdge e : incomingEdges) {
            if (e.getStart() > end) {
                break;
            }
            contacts.add(e);
        }
        return contacts;
    }
}