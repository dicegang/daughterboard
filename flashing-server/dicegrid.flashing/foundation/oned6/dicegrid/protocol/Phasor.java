package foundation.oned6.dicegrid.protocol;

public record Phasor(double magnitude, double phase) {
	public static Phasor ofCartesian(double real, double imaginary) {
		return new Phasor(Math.hypot(real, imaginary), Math.atan2(imaginary, real));
	}

	public static Phasor ofPolar(double magnitude, double angle) {
		return new Phasor(magnitude, angle);
	}

	public double real() {
		return magnitude * Math.cos(phase);
	}

	public double imaginary() {
		return magnitude * Math.sin(phase);
	}

	public Phasor scale(double factor) {
		return new Phasor(magnitude * factor, phase);
	}

	public Phasor add(Phasor other) {
		double real = real() + other.real();
		double imaginary = imaginary() + other.imaginary();
		return ofCartesian(real, imaginary);
	}

	public Phasor multiply(Phasor other) {
		double magnitude = this.magnitude * other.magnitude;
		double angle = this.phase + other.phase;
		return ofPolar(magnitude, angle);
	}
}
