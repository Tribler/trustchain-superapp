package nl.tudelft.trustchain.dna.java;

public class Machine {

    static double alpha = 0.005;

    private Pair<Double> pair;
    private double intercept; //slope //s
    private double bias;

    /**
     * Constructor, initializes the machine.
     * @param x - first argument.
     * @param y - second argument.
     */
    @SuppressWarnings("all")
    public Machine(double x, double y) {
        this.pair = new Pair<Double>(x,y);

        this.intercept = 0;
        this.bias = 0;
    }


    //TODO! WHY DOES IT NOT CONVERGE ?????
    public void updateMU(Machine other) {
        //compute the average
        double avg_intercept = (this.intercept + other.getIntercept()) * 0.5;
        double avg_bias = (this.bias + other.bias) * 0.5;


        double interm_intercept = DerivativeWithRespectToIntercept(this.pair.Y(), avg_intercept, this.pair.X(), avg_bias);
        double interm_slope = DerivativeWithRespectToSlope(this.pair.Y(), avg_intercept, this.pair.X(), avg_bias);


        //update
        this.intercept = this.intercept - (interm_slope * alpha);
        this.bias = this.bias - (interm_intercept * alpha);
    }






    public void updateUM(Machine other) {
        double interm_intercept = DerivativeWithRespectToIntercept(this.pair.Y(), this.getIntercept(), this.pair.X(), this.getBias());
        double interm_slope = DerivativeWithRespectToSlope(this.pair.Y(), this.getIntercept(), this.pair.X(), this.getBias());

        //update
        this.intercept = this.intercept - (interm_slope * alpha);
        this.bias = this.bias - (interm_intercept * alpha);

        double interm_intercept2 = DerivativeWithRespectToIntercept(this.pair.Y(), other.getIntercept(), this.pair.X(), other.getBias());
        double interm_slope2 = DerivativeWithRespectToSlope(this.pair.Y(), other.getIntercept(), this.pair.X(), other.getBias());

        //update
        double otherintercept = other.intercept - (interm_slope2 * alpha);
        double otherbias = other.bias - (interm_intercept2 * alpha);

        //only then average
        this.intercept = (this.intercept + otherintercept) * 0.5;
        this.bias = (this.bias + otherbias) * 0.5;
    }


    private double DerivativeWithRespectToIntercept(double y, double s, double x, double b){
        return (-2)*(y-s*x-b);
    }

    private double DerivativeWithRespectToSlope(double y, double s, double x, double b){
        return (-2)*x*(y-s*x-b);
    }





    //getters and setters
    public double getIntercept() {
        return intercept;
    }


    public double getBias() {
        return bias;
    }


    @Override
    public String toString() {
        return "Machine{" +
                "intercept=" + intercept +
                ", bias=" + bias +
                '}';
    }

}
