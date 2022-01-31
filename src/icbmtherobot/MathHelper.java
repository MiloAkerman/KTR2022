package icbmtherobot;

import static java.lang.Math.PI;

public class MathHelper {
    public double rad2Deg(double rads) {
        return rads / PI * 180.0;
    }
    public double deg2Rad(double degs) {
        return degs * PI / 180.0;
    }

    public double dot(double[] vec1, double[] vec2) {
        double result = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            result += vec1[i] * vec2[i];
        }
        return result;
    }
    public double magnitude(double[] vec) {
        // nth root of the sum of the nth power of each component where n = number of components
        double baseVal = 0.0;
        int dim = vec.length;
        for (int i = 0; i < dim; i++) {
            baseVal += Math.pow(vec[i], dim);
        }
        return Math.pow(baseVal, 1.0/dim);
    }

    public double angleBetween(double[] a, double[] b) {
        // θ = sin-1 [ |a × b| / (|a| |b|) ]
        // or
        // θ = cos-1 [ (a · b) / (|a| |b|) ]
        double angle = Math.acos( dot(a, b) / ( magnitude(a) * magnitude(b) ) );
        return angle;
    }
}
