package de.tum.ei.lkn.eces.webgraphgui.color;

/**
 * A color represented as RGB.
 *
 * @author Florian Kreft
 * @author Amaury Van Bemten
 */
public class RGBColor {
	private int red = 0;
	private int green = 0;
	private int blue = 0;

	//returns false if out of bounds (0-255) for color values, true if valid rgb-values
	public boolean setRGB(int newRed, int newGreen, int newBlue){
		boolean inBounds = true;
		if (newRed <= 0) { red = 0; inBounds = false; }
		else if (newRed >= 255) { red = 255; inBounds = false; }
		else { red = newRed; }

		if (newGreen <= 0) { green = 0; inBounds = false; }
		else if (newGreen >= 255) { green = 255; inBounds = false; }
		else { green = newGreen; }

		if (newBlue <= 0) { blue = 0; inBounds = false; }
		else if (newBlue >= 255) { blue = 255; inBounds = false; }
		else { blue = newBlue; }

		return inBounds;
	}

	public static String gray() {
		return RGBColor.returnRGBString(120, 120, 120);
	}

	public String returnRGB() {
		return "rgb(" + red + "," + green + "," + blue + ")";
	}

	public static String returnRGBString(int red, int green, int blue) {
		String returnString = "rgb(";
		if (red <= 0) { returnString += "0";}
		else if (red >= 255) { returnString += "255";}
		else { returnString += Integer.toString(red); }
		returnString += ",";

		if (green <= 0) { returnString += "0";}
		else if (green >= 255) { returnString += "255";}
		else { returnString += Integer.toString(green); }
		returnString += ",";

		if (blue <= 0) { returnString += "0";}
		else if (blue >= 255) { returnString += "255";}
		else { returnString += Integer.toString(blue); }
		returnString += ")";

		return returnString;
	}

	/**
	 * Returns a String representation of a color (in the form
	 * "rgb(XXX, YYY, ZZZ)"). Based on the percent value, the color will be
	 * a mix of green and red (0% is green, 50% is orange, 100% is red). Returns
	 * green if the percentage is negative.
	 * @param percent Percent value [0, 1].
	 * @return The String representation of the color.
	 */
	static public String percentToColor(double percent) {
		percent = percent * 100;
		if(percent < 0)
			return RGBColor.returnRGBString(0, 255, 0);
		else if(percent < 50)
			return RGBColor.returnRGBString((int) ((255.0 * percent * 2) / 100), 255, 0);
		else if(percent < 100)
			return RGBColor.returnRGBString(255, 255 - (int) ((255.0 * (percent - 50) * 2) / 100), 0);
		else
			return RGBColor.returnRGBString(255, 0, 0);
	}
}
