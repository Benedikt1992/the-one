package routing.contactgraph;

public class ContactPlanEdge {
    private ContactPlanNode from;
    private ContactPlanNode to;
    private double start;
    private double end;

    public ContactPlanEdge(ContactPlanNode from, ContactPlanNode to, double start, double end) {
        this.from = from;
        this.to = to;
        this.start = start;
        this.end = end;
    }

    public ContactPlanNode getFrom() {
        return from;
    }

    public ContactPlanNode getTo() {
        return to;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }
}
