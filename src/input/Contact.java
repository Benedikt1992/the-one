package input;

public class Contact {
	private Integer partner;
	private Double start;
	private Double end;

	public Contact(Integer partner, Double start, Double end) {
		this.partner = partner;
		this.start = start;
		this.end = end;
	}

	public Integer getPartner() {
		return partner;
	}

	public Double getStart() {
		return start;
	}

	public Double getEnd() {
		return end;
	}
}
