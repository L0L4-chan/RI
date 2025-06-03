package es.udc.fi.ri.practica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.apache.lucene.queryparser.classic.ParseException;

public class compare {
/**
 * Funcion principal de la clase compare 
 * @param args
 * @throws IOException
 * @throws ParseException
 */
public static void main(String[] args) throws IOException, ParseException {
	//string the ayuda en caso de fallo
	    String usage =
	        "SearchEvalTreeCovid"
	            + "  [-tTest|wilcoxon ALPHA_VALUE] [-results FILE_NAME_1 FILE_NAME_2]\n";
		
		if (args.length < 5) {
			System.out.println("More parametres are needed");
			  System.err.println("Usage: " + usage);
		      System.exit(1);
		}
	
		//Declaración de variables
		
		Boolean tTest = false;
		Boolean wilcoxon = false; 
		Double alpha = 0.;
		String results1= null;
		String results2= null;
		
		
	    //Inicialización de variables con argumentos
	    for (int i = 0; i < args.length; i++) {
	      switch (args[i]) {
	        case "-tTest":
	        	tTest = true;
	        	alpha = Double.valueOf(args[++i]);
	          break;
	        case "-wilcoxon":
	        	wilcoxon = true;
	        	alpha = Double.valueOf(args[++i]);
	          break;
	        case "-results":
	          results1 = args[++i];
	          results2 = args[++i];
	          break;
	        default:
	          throw new IllegalArgumentException("unknown parameter " + args[i]);
	      }
	    }
	    
	    if ((tTest && wilcoxon) || (!tTest && !wilcoxon)) {
            System.err.println("Please specify either -tTest or -wilcoxon");
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        if (results1 == null || results2 == null) {
            System.err.println("Results files not specified");
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        // Leer los resultados
        double[] model1Results = readResults(results1);
        double[] model2Results = readResults(results2);
        // Se hacen los test pertinentes
        if (tTest) {
            performTTest(model1Results, model2Results, alpha);
        } else {
           performWilcoxonTest(model1Results, model2Results, alpha);
        }
    }

/**
* Funcion que recupera  de los archivos los datos solicitados
 * @param fileName
 * @return un array de doubles 
 * @throws IOException
 */
private static double[] readResults(String fileName) throws IOException {
	    	ArrayList<Double> stats = new ArrayList<Double>();
	    	int i=0;
	    	 BufferedReader reader = new BufferedReader(new FileReader(fileName));
		        String line;
		        while ((line = reader.readLine()) != null) {          	     
	    	        String[] parts = line.split(",");
	    	        if (!(parts[1].compareToIgnoreCase("promedios")==0)&&
	    	        		!(parts[1].compareToIgnoreCase("PN")==0)&&
	    	        		!(parts[1].compareToIgnoreCase("RecallN")==0)&&	
	    	        		!(parts[1].compareToIgnoreCase("MAPN")==0)&&
	    	        		!(parts[1].compareToIgnoreCase("MRR")==0)
	    	        		
	    	        		) {
	                   	stats.add(i++, Double.valueOf(parts[1]));
	    		    }
	    	    }
	    	    
		        reader.close();
		        
	    	    return toArr(stats);
	    	       
	    	   	}
	    
	    
/**
 * Convierte una lista de doubles en un array de dobles	    
 * @param toFill
 * @return un array de dobles	  
 */
private static double[] toArr(List<Double> toFill ) {
	    	double[] result = new double[toFill.size()];
	    	
	        for (int i = 0; i < toFill.size(); i++) {
	            result[i] = toFill.get(i);
	        }
			return result;
	    	
	    }
/**
 * Funcion que llama al test de comparacion de bondades e imprime por pantalla si se acepta o rechaza la hipotesis nula.
 * @param objects
 * @param objects2
 * @param alpha
 */
	    private static void performTTest(double[] objects, double[] objects2, double alpha) {
	    	TTest tTest = new TTest();
	    	double pValue = tTest.pairedTTest(objects, objects2);
	        System.out.println("T-Test Result:");;
	        System.out.println("p-value: " + pValue);
	        if (pValue < alpha) {
	            System.out.println("Reject null hypothesis: There is a significant difference between the models.");
	        } else {
	            System.out.println("Acept null hypothesis: There is no significant difference between the models.");
	        }
	    }
/**
 * Funcion que llama al test de comparacion de bondades e imprime por pantalla si se acepta o rechaza la hipotesis nula.
 * @param objects
 * @param objects2
 * @param alpha
 */
private static void performWilcoxonTest(double[] model1Results, double[] model2Results, double alpha) {
        WilcoxonSignedRankTest test = new WilcoxonSignedRankTest();
        double pValue = test.wilcoxonSignedRankTest(model1Results, model2Results, false);
        System.out.println("Wilcoxon Signed-Rank Test Result:");
        System.out.println("p-value: " + pValue);
        if (pValue < alpha) {
            System.out.println("Reject null hypothesis: There is a significant difference between the models.");
        } else {
            System.out.println("Acept null hypothesis: There is no significant difference between the models.");
        }
    }
}