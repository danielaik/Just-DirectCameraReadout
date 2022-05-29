/*
 * 2D Gaussian fitter adapted from Yoshiyuki Arai
 */
package directCameraReadout.iccs;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

public class TwoDGaussFunction {

    // Member variables
    int data_width; // width of data
    int data_size; // data size

    double[] newStart;
    double[] data;
    int[] optim_param;

    LeastSquaresProblem lsp;

    /**
     * @param data	input data
     * @param data_width	input data width
     */
    public TwoDGaussFunction(double[] data, double[] newStart, int data_width, int[] optim_param) {
        //construct two-dimensional Gaussian function
        this.data = data;
        this.newStart = newStart;
        this.data_width = data_width;
        this.optim_param = optim_param;
        this.data_size = data.length;
        buildlsb();

    }

    /**
     * build LeastSquareProblem by using constructer data
     */
    private void buildlsb() {

        //prepare construction of LeastSquresProblem by builder
        LeastSquaresBuilder lsb = new LeastSquaresBuilder();

        //set model function and its jacobian
        lsb.model(this.retMVF(), this.retMMF());
        //set target data
        lsb.target(data);
        //set initial parameters
        lsb.start(newStart);
        //set upper limit of evaluation time
        lsb.maxEvaluations(optim_param[0]);
        //set upper limit of iteration time
        lsb.maxIterations(optim_param[1]);

        lsp = lsb.build();
    }

    public MultivariateVectorFunction retMVF() {
        return new MultivariateVectorFunction() {

            @Override
            public double[] value(double[] v)
                    throws IllegalArgumentException {
                double[] values = new double[data_size];
                // pre-calculation
                double v3v3 = v[3] * v[3];
                double v4v4 = v[4] * v[4];
                double sqrt_twopiv3v4 = Math.sqrt(2 * Math.PI * v3v3 * v4v4);
                for (int i = 0; i < values.length; ++i) {
                    // parameters for x,y positioning
                    int xi = i % data_width;
                    int yi = i / data_width;
                    /*
		        	 * f(x,y) = A/sqrt(2*pi*s_x^2*s_y^2)*exp(-(x-x_m)^2/(2*s_x^2))*exp(-(y-y_m)^2/(2*s_y^2))+offset
		        	 * v[0] : A
		        	 * v[1] : x_m	mean of x
		        	 * v[2] : y_m	mean of y
		        	 * v[3] : s_x	sigma of x
		        	 * v[4] : s_y	sigma of y
		        	 * v[5] : offset	offset
                     */
                    values[i] = v[0] / sqrt_twopiv3v4
                            * Math.exp(-(xi - v[1]) * (xi - v[1]) / (2 * v3v3))
                            * Math.exp(-(yi - v[2]) * (yi - v[2]) / (2 * v4v4))
                            + v[5];
                }
                return values;
            }
        };
    }

    /**
     * Return the jacobian of the model function
     *
     * @return	return the jacobian
     */
    public MultivariateMatrixFunction retMMF() {
        return new MultivariateMatrixFunction() {

            @Override
            public double[][] value(double[] point)
                    throws IllegalArgumentException {
                return jacobian(point);
            }

            private double[][] jacobian(double[] v) {
                double[][] jacobian = new double[data_size][6];
                double v3v3 = v[3] * v[3];
                double v4v4 = v[4] * v[4];
                double sqrt_twopiv3v4 = Math.sqrt(2 * Math.PI * v3v3 * v4v4);
                for (int i = 0; i < jacobian.length; ++i) {
                    // parameters for x,y positioning
                    int xi = i % data_width;
                    int yi = i / data_width;
                    double exp_x = Math.exp(-(xi - v[1]) * (xi - v[1]) / (2 * v3v3));
                    double exp_y = Math.exp(-(yi - v[2]) * (yi - v[2]) / (2 * v4v4));
                    //partial differentiations were calculated by using Maxima
                    jacobian[i][0] = exp_x * exp_y / sqrt_twopiv3v4;								//df(x,y)/dv0
                    jacobian[i][1] = v[0] * (xi - v[1]) / v3v3 * jacobian[i][0]; 						//df(x,y)/dv1
                    jacobian[i][2] = v[0] * (yi - v[2]) / v4v4 * jacobian[i][0]; 						//df(x,y)/dv2
                    jacobian[i][3] = jacobian[i][1] * (xi - v[1]) / v[3] - v[0] * jacobian[i][0] / v[3]; 	//df(x,y)/dv3
                    jacobian[i][4] = jacobian[i][2] * (yi - v[2]) / v[4] - v[0] * jacobian[i][0] / v[4];	//df(x,y)/dv4
                    jacobian[i][5] = 1;															//df(x,y)/dv5
                }
                return jacobian;
            }
        };
    }

    /**
     * Do two dimensional Gaussian fit
     *
     * @return return the fitted data as Optimum
     */
    public LeastSquaresOptimizer.Optimum fit2dGauss() {
        LevenbergMarquardtOptimizer lmo = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum lsoo = lmo.optimize(lsp);

        return lsoo;
    }

}
