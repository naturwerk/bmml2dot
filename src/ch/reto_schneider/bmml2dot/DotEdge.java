package ch.reto_schneider.bmml2dot;

class DotEdge {
	public String destinationFilename;
	public String sourceFilename;
	public String hrefText;

	public DotEdge(String destinationFilename, String sourceFilename,
			String text) {
		hrefText = text.trim();
		hrefText = hrefText.replaceAll(">", "Menüpunkt:");
		hrefText = hrefText.replaceAll("\n", " ");

		this.sourceFilename = sourceFilename;
		this.destinationFilename = destinationFilename;
	}
}
