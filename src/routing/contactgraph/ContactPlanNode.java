package routing.contactgraph;

import java.util.*;

public class ContactPlanNode  extends ContactGraphNode {
    private Integer address;
    private List<ContactPlanEdge> incomingEdges;
    private List<ContactPlanEdge> outgoingEdges;
    private boolean incomingSorted;
    private boolean incomingSortedEnd;
    private boolean outgoingSorted;

    public ContactPlanNode(Integer address) {
        super();
        this.address = address;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.incomingSorted = false;
        this.incomingSortedEnd = false;
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
        incomingSortedEnd = false;
    }

    private void sortIncomingEdges() {
        if (!incomingSorted) {
            Collections.sort(incomingEdges, Comparator.comparingDouble(ContactPlanEdge::getStart));
            incomingSortedEnd = false;
            incomingSorted = true;
        }
    }

    private void sortIncomingEdgesOnEnd() {
        if (!incomingSortedEnd) {
            Collections.sort(incomingEdges, Comparator.comparingDouble(ContactPlanEdge::getEnd));
            incomingSortedEnd = true;
            incomingSorted = false;
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

    public List<ContactPlanEdge> getPreviousContacts(ContactPlanEdge edge, Double end) {
        List<ContactPlanEdge> contacts = new ArrayList<>();
        sortIncomingEdgesOnEnd();
        for (ContactPlanEdge e : incomingEdges) {
            if (e.getEnd() > end) {
                break;
            }
            contacts.add(e);
        }
        return contacts;
    }
}