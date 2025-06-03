package es.udc.fi.ri.practica;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import com.opencsv.CSVWriter;

import org.apache.lucene.queryparser.classic.ParseException;


public class TrainingTestTrecCovid {	
/**
 * Funcion inicial del codifo
 * @param args
 * @throws Exception
 */
public static void main(String[] args) throws Exception {
				
	 //Variables necesarias podemos inicializarlas en el main y pasarlas como parametros si es necesario
	Boolean evaljm = false;
	Boolean evalbm25 = false; 
	String p = null;
	String q = null;
	Integer int1 = 0;
	Integer int2 = 0;
	Integer int3 = 0;
	Integer int4 = 0;
	String index = null;
	Integer cut = 0;
	String metrica = null;
	
	String usage =
	    "TrainingTestTrecCovid"
	        + " [-index INDEX_PATH] [-evaljm | -evalbm25] [-cut CUT_MODE] [-metrica METRICA_MODE] \n\n";
	   
	//comprobamos que tenemos los argumentos necesarios
	if (args.length <7) {
		System.out.println("A folder is needed for the index");
		  System.err.println("Usage: " + usage);
	      System.exit(1);
	}
	
			    
	for (int i = 0; i < args.length; i++) {
	  switch (args[i]) {
	    case "-index":
	      index = args[++i];
	      break;
	    case "-evaljm":
	          evaljm = true;
	          p = args[++i];
	          q = args[++i];
	          String[] queries1 = p.split("-");
	          int1 =Integer.valueOf( queries1[0]);
	          int2 = Integer.valueOf( queries1[1]);
	          String[] queries2 = q.split("-");
	          int3 =Integer.valueOf( queries2[0]);
	          int4 = Integer.valueOf( queries2[1]);
	      break;
	    case "-evalbm25":
	          evalbm25 = true;
	          p = args[++i];
	          q = args[++i];
	          String[] queries11 = p.split("-");
	          int1 =Integer.valueOf( queries11[0]);
	          int2 = Integer.valueOf( queries11[1]);
	          String[] queries21 = q.split("-");
	          int3 =Integer.valueOf( queries21[0]);
	          int4 = Integer.valueOf( queries21[1]);
	      break;	          
	    case "-cut":
	    	cut = Integer.valueOf(args[++i]);
	      break;
	    case "-metrica":
	    	metrica = args[++i];
	    	break;
	    default:
	      throw new IllegalArgumentException("unknown parameter " + args[i]);
	  }
	}
	
	if (index == null) {
	  System.err.println("Usage: " + usage);
	  System.exit(1);
	}
	
	if( (evaljm == true && evalbm25 == true)||(evaljm == false && evalbm25 == false)) {
	      System.err.println(" evaljm and evalbm25 are not compatible. Only one option can be loaded");
	      System.exit(1);
	      }
	List<String> arg = new ArrayList<String>();
	 arg.add("-index");
	 arg.add(index);
	 arg.add("-queries");
	 arg.add(p);
	 arg.add("-cut");
	 arg.add(String.valueOf(cut));
	 arg.add("-top");
	 arg.add(String.valueOf(100));
	 arg.add("-search");
	List<String> arg2 =  new ArrayList<String>();
	arg2.add("-index");
	 arg2.add(index);
	 arg2.add("-queries");
	 arg2.add(q);
	 arg2.add("-cut");
	 arg2.add(String.valueOf(cut));
	 arg2.add("-top");
	 arg2.add(String.valueOf(100));
	 arg2.add("-search");
	    
	 // Ejecutar proceso de entrenamiento o prueba
	if (evaljm) {
		
		 arg.add("jm");
		 arg2.add("jm");
		 HashMap<Double, HashMap<String, Double>> resultadosEntrenamiento = probarHiperparametros(arg, evaljm, metrica) ;
	     String prom = escribirResultados("target/classes/jm.training." + p+".test."+q+"."+metrica+cut+".trainig.csv", resultadosEntrenamiento, evaljm, int1, int2);
	     arg2.add(prom);
	     SearchEvalTrecCovid.main(arg2.toArray(new String[11]));
	     String path= "TREC-COVID."+arg2.get(9)+"."+arg2.get(5)+".hits.lambda."+prom+".q."+arg2.get(3)+".csv";
	     List<Estadisticos> st = readStat(path);
	     HashMap<String, Double> resultadosTest = writeStat(metrica,st);
	     escribirResultados2("target/classes/jm.training." + p+".test."+q+"."+metrica+cut+".test.csv", resultadosTest, metrica, prom , int3, int4);
	      
		
	} else if (evalbm25) {
		//crear el string de los argumentos
		 arg.add("bm25");
		 arg2.add("bm25");
		 
		 HashMap<Double, HashMap<String, Double>> resultadosEntrenamiento = probarHiperparametros(arg, evaljm, metrica) ;
	     String prom = escribirResultados("target/classes/bm25.training." + p+".test."+q+"."+metrica+cut+".trainig.csv", resultadosEntrenamiento, evaljm, int1, int2);
	      arg2.add(prom);
	      SearchEvalTrecCovid.main(arg2.toArray(new String[11]));              
	      String path= "TREC-COVID."+arg2.get(9)+"."+arg2.get(5)+".hits.k1."+prom+".q."+arg2.get(3)+".csv";
	      List<Estadisticos> st = readStat(path);
	      HashMap<String, Double> resultadosTest = writeStat(metrica,st);
	      escribirResultados2("target/classes/bm25.training." + p+".test."+q+"."+metrica+cut+".test.csv", resultadosTest, metrica, prom , int3, int4);
	      
		  
	} else {
	    System.err.println("Please specify either -evaljm or -evalbm25");
	        System.exit(1);
	    }
	}

/**
 * Funcion que realiza los entrenamientos con los diferentes hiperparametros
 * @param arg1
 * @param evaljm
 * @param metrica
 * @return para cada hiperparametro, un hash con la query y su resultado en lametrica pedida
 * @throws IOException
 * @throws ParseException
 */
private static HashMap<Double, HashMap<String, Double>> probarHiperparametros(List<String> arg1, boolean evaljm ,String metrica) throws IOException, ParseException{
	HashMap<Double, HashMap<String, Double>> resultados = new HashMap< Double,HashMap<String, Double>>();
	 if (evaljm) {
		 Double[] parametros  = {0.001,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
			for(Double p : parametros ) {
				arg1.add(String.valueOf(p));
				 SearchEvalTrecCovid.main(arg1.toArray(new String[11]));
				 String path= "TREC-COVID."+arg1.get(9)+"."+arg1.get(5)+".hits.lambda."+p+".q."+arg1.get(3)+".csv";	
			 List<Estadisticos> st = readStat(path);	
			 resultados.put(p, (writeStat(metrica,st)));
		    arg1.remove(String.valueOf(p));
		    	
		}
 }else {
	 Double[] parametros = {0.4,0.6,0.8,1.0,1.2,1.4,1.6,1.8,2.0};
		for(Double p : parametros ) {
			arg1.add(String.valueOf(p));
			 SearchEvalTrecCovid.main(arg1.toArray(new String[11]));
			 String path= "TREC-COVID."+arg1.get(9)+"."+arg1.get(7)+".hits.k1."+p+".q."+arg1.get(3)+".csv";	
				 List<Estadisticos> st = readStat(path);	
			    resultados.put(p,writeStat(metrica,st));
			    arg1.remove(String.valueOf(p));
			    	
			}
	 }
	return resultados;
	
}
/**
 * Devuelve los resultados de la metrica que se ha pedido
 * @param metrica
 * @param st
 * @return
 */
private static HashMap<String, Double>  writeStat(String metrica,List<Estadisticos> st) {
		HashMap<String, Double> resultado = new HashMap<String, Double>();
		switch(metrica) {
		case "PN": 
		 	for(Estadisticos s : st) {
		 		resultado.put(s.query(), s.PN());
		 	}
		 	return resultado;
	case "RecallN":
		for(Estadisticos s : st) {
	 		resultado.put(s.query(),s.RecallN());
	 	}
	 	return resultado;
	case "MAPN":
		for(Estadisticos s : st) {
	 		resultado.put(s.query(), s.MAPN());
	 	}
	 	return resultado;
	case "MRR": 
		for(Estadisticos s : st) {
	 		resultado.put(s.query(), s.MRR());
	 	}
	 	return resultado;
	default:
		 throw new IllegalArgumentException("unknown parameter " + metrica);	
		}
	}

/**
 * Lee del csv pertinente los datos 
 * @param path
 * @return
 */
private static  List<Estadisticos> readStat(String path) {
	 
    var is = TrainingTestTrecCovid.class.getResourceAsStream("/"+path);	        
List<Estadisticos> stats= new ArrayList<Estadisticos>();

try (Scanner scanner = new Scanner(is)) {
    while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] parts = line.split(",");
        if (!(parts[0].compareToIgnoreCase(" query ")==0)) {
        
         if (parts[0].compareToIgnoreCase("promedios")==0) {
	                if (parts.length == 5) {
	                	String query = parts[0];
	                    double PN = Double.parseDouble(parts[1]);
	                    double RecallN =  Double.parseDouble(parts[2]);;
	                    double MAPN =  Double.parseDouble(parts[3]);;
	                    double MRR =   Double.parseDouble(parts[4]);;
	                   stats.add( new Estadisticos(query,PN, RecallN, MAPN, MRR));
	                }
                }else {
                	String query = parts[0];
                    double PN = Double.parseDouble(parts[1]);
                    double RecallN =  Double.parseDouble(parts[2]);;
                    double MAPN =  Double.parseDouble(parts[3]);;
                    double MRR =   Double.parseDouble(parts[4]);;
                    stats.add( new Estadisticos(query,PN, RecallN, MAPN, MRR));
                }
            }
        }
            return stats;
       
   	}
     
}
/**
 * Crea el documento resultado de entrenamiento
 * @param nombreArchivo
 * @param resultadosEntrenamiento
 * @param jm
 * @param start
 * @param end
 * @return
 * @throws IOException
 */
private static String escribirResultados(String nombreArchivo, HashMap<Double, HashMap<String, Double>> resultadosEntrenamiento, boolean jm, int start, int end) throws IOException {
        
     char delimitador = ','; // Coma como delimitador
    char quotechar = '"';    // Carácter de comillas
    char escapechar = '\\';  // Carácter de escape
    String lineEnd = "\n";   // Terminador de línea
    List<Double> toFill= new ArrayList<Double>();
    List<Double> prom= new ArrayList<Double>();
    double max= 0;
	String toReturn= "";
    
    try (CSVWriter writer = new CSVWriter(new FileWriter(nombreArchivo, true), delimitador, quotechar, escapechar, lineEnd)) {
    	if(jm) {
    		String [] line = {"Query", "0.001", "0.1", "0.2", "0.3","0.4","0.5","0.6","0.7","0.8","0.9","1.0"};
    		writer.writeNext(line, false);
    		String[] aux = { "0.001", "0.1", "0.2", "0.3","0.4","0.5","0.6","0.7","0.8","0.9","1.0"} ;
    		
    			for( int i = start; i<=end; i++) {
    				toFill.add(Double.valueOf(i));
    				for (String s : aux) {
    					toFill.add(resultadosEntrenamiento.get(Double.valueOf(s)).get(String.valueOf(i)));
    						if(i==end) {
    							double now= resultadosEntrenamiento.get(Double.valueOf(s)).get("promedios");
    							prom.add(now);
    							if(now > max) {
    								max = now;
    								toReturn = s;
    							}
    						}
    					}
    				String[] lin;
					try {
						lin = toArr(String.valueOf(i),toFill);
						toFill.clear();
						writer.writeNext(lin , false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				
    		}
    		
    	}else {
    		String [] line = {"Query", "0.4", "0.6", "0.8", "1.0","1.2","1.4","1.6","1.8","2.0"};
    		writer.writeNext(line, false);
    		String[] aux = {"0.4", "0.6", "0.8", "1.0","1.2","1.4","1.6","1.8","2.0"} ;
    		for( int i = start; i<=end; i++) {
    				for (String s : aux) {
    					toFill.add(resultadosEntrenamiento.get(Double.valueOf(s)).get(String.valueOf(i)));
    					if(i==end) {
    						double now= resultadosEntrenamiento.get(Double.valueOf(s)).get("promedios");
							prom.add(now);
    							if(now > max) {
    								max = now;
    								toReturn = s;
    							}
    						}
    					}
    				String[] lin;
				try {
					lin = toArr(String.valueOf(i),toFill);
					toFill.clear();
					writer.writeNext(lin , false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				}
    		}
    	try {
    		String[] lin = toArr("promedios", prom);
			writer.writeNext(lin , false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    		
    } catch (IOException e) {
        System.out.println("Impossible to write on file");
  		    }
  		    
  		    return toReturn;
  	        }
    
/**
 * Funcion auxiliar que convierte una lista en un array de String y añade al inicio un string (se podria sustituir)     
 * @param string
 * @param toFill
 * @return
 */
private static String[] toArr(String string, List<Double> toFill ) {
    	
    	String[] result = new String[13];
    	result[0] = string;
        for (int i = 0; i < toFill.size(); i++) {
            result[i+1] = String.valueOf(toFill.get(i));
        }
		return result;
    	
    }
    
    
/**
 * Funcion que escribe en el documento conclusion de los training    
 * @param nombreArchivo
 * @param resultadosEntrenamiento
 * @param metrica
 * @param lambda
 * @param start
 * @param end
 * @throws IOException
 */
private static void escribirResultados2(String nombreArchivo,  HashMap<String, Double> resultadosEntrenamiento,String metrica , String lambda, int start, int end) throws IOException {
        

			char delimitador = ','; // Coma como delimitador
    char quotechar = '"';    // Carácter de comillas
    char escapechar = '\\';  // Carácter de escape
    String lineEnd = "\n";   // Terminador de línea

    
    try (CSVWriter writer = new CSVWriter(new FileWriter(nombreArchivo, true), delimitador, quotechar, escapechar, lineEnd)) {
    		String[] a ={lambda, metrica};
    		writer.writeNext(a , false);
    
    	for( int i = start; i<=end; i++) {
			
			String[] lin = {String.valueOf(i),String.valueOf(resultadosEntrenamiento.get(String.valueOf(i)))};
			writer.writeNext(lin , false);

    	}
    	String[] lin = {"promedios",String.valueOf(resultadosEntrenamiento.get(String.valueOf("promedios")))};
		writer.writeNext(lin , false);
    	
    	
    } catch (IOException e) {
        System.out.println("Impossible to write on file");
  		    }
  		    
  	        }
    
}
	    

