package ch.reto_schneider.bmml2dot;

class DotEdge {
	public String destinationFilename;
	public String sourceFilename;
	public String linkLabel;

	public DotEdge(String destinationFilename, String sourceFilename,
			String text) {
		linkLabel = text.trim();
		linkLabel = linkLabel.replaceAll(">", "Menüpunkt:");
		linkLabel = linkLabel.replaceAll("\n", " ");

		this.sourceFilename = sourceFilename;
		this.destinationFilename = destinationFilename;
	}
}
