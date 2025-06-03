package es.udc.fi.ri.practica;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.opencsv.CSVWriter;

public class SearchEvalTrecCovid {
private static String usage = null;

/**
 * Funcion principal de la clase que ejecuta la busqueda
 * @param args
 * @throws IOException
 * @throws ParseException
 */
	 public static void main(String[] args) throws IOException, ParseException {
		//string the ayuda en caso de fallo
		   usage =
		        "SearchEvalTreeCovid"
		            + "  [-index INDEX_PATH] [-search jm <lambda> | bm25 <k1>][-cut n] [-top topN] [-queries  all | <int1> | <int1-int2> ] \n";
			
			if (args.length < 11) {
				System.out.println("More parametres are needed");
				  System.err.println("Usage: " + usage);
			      System.exit(1);
			}
			//Declaración de variables
			String indexPath = null;
	        int  topN = 10;
	        int  cut = 10;
	        String q= null;
	        boolean all = false;
	        int  start = 0;
	        int  end = 0;
	    	String iModel = null;
	    	float lambda = 0;
	    	float k1 = 0;
	    	String forPath= null;;
   //Inicialización de variables con argumentos
		    for (int i = 0; i < args.length; i++) {
		      switch (args[i]) {
		        case "-index":
		          indexPath = args[++i];
		          break;
		        case "-top":
		          topN =  Integer.valueOf(args[++i]);
		          break;
		        case "-cut":
			          cut =  Integer.valueOf(args[++i]);
			          break;
		        case "-queries":
			          q =  args[++i];
			          if(q.equalsIgnoreCase("all")) {
			        	  all = true;
			          }else if (q.contains("-")) {
			        	 String[] queries = q.split("-");
			        	 start=Integer.valueOf( queries[0])-1;
			        	 end = Integer.valueOf( queries[1])-1;
			        	 
			        	  if (end<start) {
			        		  System.err.println("Query selecction is incorrect");
			  	              System.exit(1);
			        	  }
			        	  
			          }else {
			        	  start = end =  Integer.valueOf(q)-1;
			          }
			          break;
		        case "-search":
		        	iModel=args[++i];
		          if (iModel.equals("jm")){
				  lambda = Float.valueOf(args[++i]);
				  forPath = "lambda."+lambda;
				  }else {
				  k1 = Float.valueOf(args[++i]);
				  forPath = "k1."+k1;
			      }
		          break;
		        default:
		          throw new IllegalArgumentException("unknown parameter " + args[i]);
		      }
		    }
		    
		    if (indexPath == null) {
		        System.err.println("Usage: " + usage);
		        System.exit(1);
		      }
	        
	        //tratar con las queries
	        
	        List<Query> toExam = selectQueries(all, start, end); 
		    Directory dir;
			DirectoryReader indexReader;
			IndexSearcher indexSearcher;
			Analyzer analyzer;
			try {
				dir = FSDirectory.open(Paths.get(indexPath));
				indexReader = DirectoryReader.open(dir);
		        indexSearcher = new IndexSearcher(indexReader);
		        analyzer  = (new StandardAnalyzer());
		        String path= "target/classes/TREC-COVID."+iModel+"."+topN+".hits."+forPath+".q."+q+".txt";
				String path2= "target/classes/TREC-COVID."+iModel+"."+topN+".hits."+forPath+".q."+q+".csv";
				String[] toSa =  {" query " , " P@N "," Recall@N ", " MAP@N ", " MRR "};
				writeToFile2(path2, toSa);				
				double[] promedios = {0,0,0,0};
		        if (iModel.equalsIgnoreCase("jm")) {
		        	indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));
		             } else if(iModel.equalsIgnoreCase("bm25")) {  // Add new documents to an existing index:
		            	 indexSearcher.setSimilarity(new BM25Similarity(k1,(float) 0.75));
		             } else {
		            	 System.err.println("Wrong similarity Mode");
				         System.exit(1);
		             }
		   
				for(Query qr : toExam) {
					String queryText= qr.metadata().query().toLowerCase();
	                TopDocs topDocs = null;
	                MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[]{"text"}, analyzer);
					try {
						topDocs = indexSearcher.search(parser.parse(queryText), topN);
					} catch (IOException | ParseException e) {
						System.err.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
				         System.exit(1);
					}
					HashMap<String, Integer> judgmentsMap = toHash( loadJudgments(Integer.valueOf(qr.id())));
					
					double[] aux = calculate(indexReader, topDocs, judgmentsMap, cut,qr,path, path2, iModel, lambda, k1 );
					promedios = sumados(promedios, aux);
				}
			  
			        indexReader.close();
			        dir.close();
			        
			        String[] toSave={ "promedios",String.valueOf(promedios[0]/toExam.size()), String.valueOf(promedios[1]/toExam.size()), 
			        		String.valueOf(promedios[2]/toExam.size()) , String.valueOf(promedios[3]/toExam.size())};
			        writeToFile2(path2, toSave);
			        
			} catch (CorruptIndexException e1) {
				System.out.println(" caught a " + e1.getClass() + "\n with message: " + e1.getMessage());
				e1.printStackTrace();
				return;
			} catch (IOException e1) {
				System.out.println(" caught a " + e1.getClass() + "\n with message: " + e1.getMessage());
				e1.printStackTrace();
				return;
			}
	    }
/**
 * Función auxiliar para la suba de los array miembro a miembro
 * @param promedios
 * @param aux
 * @return
 */
 private static double[] sumados(double[] promedios, double[] aux) {
		if(promedios== null) {
			return aux;
		}else {
			for (int i = 0; i < aux.length; i++) {
				promedios[i]= promedios[i] + aux[i];	
			}
			return promedios;
		}
		
	}

/**
  * Funcion que revisa las queries para evitar problemas de vocabulario
  * @param all boolean que indica si se seben usar todas las queries de la lista
  * @param start Indice de la primera query a revisar
  * @param end Indice de la ultima query a revisar
  * @param queries lista de todas las queries parseadas del jason
  * @return
 * @throws IOException 
  */
 private static List<Query> selectQueries(boolean all, int start, int end) throws IOException {
	    	var is = SearchEvalTrecCovid.class.getResourceAsStream( "/trec-covid/queries.jsonl");
	        ObjectReader reader = JsonMapper.builder().findAndAddModules().build()
	                .readerFor(Query.class);
	        
	        List<Query> queries = reader.<Query>readValues(is).readAll();
	        int aux = queries.size();
	        if(start>aux || end >aux) {
	        	  System.err.println("Queries out of range \n Usage: " + usage);
			        System.exit(1);
	        }
	    	
	    	if(all) {
	    		 return queries;
	    		
	    	}else {
	    		List<Query> selection = new ArrayList<Query>();
	    		for (int i = start; i<= end; i++) {
	    			selection.add(queries.get(i));
	    		}
	    		return selection;
	    	}
	}

/**
 *Funcion que devuelve los juicios de valor de la query que se esta analizando 
 * @int query id
 * @return Lista de Judgment
 * @throws IOException
 */
 private static List<Judgments> loadJudgments(int start) throws IOException {

    var is = SearchEvalTrecCovid.class.getResourceAsStream( "/trec-covid/qrels/test.tsv");	        
    List<Judgments>jmts = new ArrayList<Judgments>();
    
    try (Scanner scanner = new Scanner(is)) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split("\t");
            if (!(parts[0].compareToIgnoreCase("query-id")==0)) {
                if (parts.length == 3) {
                    int queryId = Integer.parseInt(parts[0]);
                    String corpusId = parts[1];
                    int score = Integer.parseInt(parts[2]);
                   jmts.add(new Judgments(queryId, corpusId, score));
                }
            }
        }
        List<Judgments> selection = new ArrayList<Judgments>();
		for (Judgments j : jmts) {
			if (j.query()== start) {
				selection.add(j);
			}
		}
		return selection;
		}
   }
/**
 * Comvierte una lista en hashmap
 * @param jmts
 * @return
 */
 private static HashMap<String, Integer> toHash (List<Judgments> jmts){
	
	 HashMap<String, Integer> selection = new HashMap<String, Integer>();
		for (Judgments j : jmts) {
			if (j.score()>0) {
				selection.put(j.corpus(),j.score());
			}
		}
		return selection;
 }
	    

/**
 * Funcion que escribe en archivo txt
 * @param filepath path del archivo que debe escribir
 * @param line linea que añade al archivo
 * @throws IOException
 */
 private static void writeToFile(String filepath, String line) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filepath), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        writer.write(line);
        writer.newLine();
    }catch(IOException e){
		System.out.println("Impossible to write on file");
}
}
 /**
  * Funcion que escribe en cvs
  * @param filepath
  * @param line
  * @throws IOException
  */
 private static void writeToFile2(String filepath, String[] line) throws IOException {
	    char delimitador = ','; // Coma como delimitador
	    char quotechar = '"';    // Carácter de comillas
	    char escapechar = '\\';  // Carácter de escape
	    String lineEnd = "\n";   // Terminador de línea

	    try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true), delimitador, quotechar, escapechar, lineEnd)) {
	        writer.writeNext(line, false);
	    } catch (IOException e) {
	        System.out.println("Impossible to write on file");
	    }
	}

/**
 * Funcion que calcula las metricas para cada query
 * @param indexReader
 * @param topDocs
 * @param judgmentsMap
 * @param cut
 * @param q
 * @param path
 * @param path2
 * @param iModel
 * @param lambda
 * @param k1
 * @return
 * @throws IOException
 */
public static double[] calculate(IndexReader indexReader ,TopDocs topDocs, HashMap<String, Integer> judgmentsMap,int cut, Query q, String path, String path2, String iModel, float lambda, float k1 ) throws IOException  {
	    int relevantes = 0;  
	    double acumulador = 0;
	    double reverse = 0;
	    int aux = Math.min(cut, topDocs.scoreDocs.length);
	    for (int i = 0 ; i < aux; i++) {
			Document auc = indexReader.storedFields().document(topDocs.scoreDocs[i].doc) ;
	    // scoreDoc.doc contiene el número de documento
		    if( judgmentsMap.containsKey(  auc.getValues("id")[0])) {
		    	relevantes ++;
		    	acumulador = acumulador+ ((double)relevantes/(i+1));  
		    	reverse = reverse +(double) (1.0 /(i+1)); 
		    }
	    }
	        // Calculate metrics
            double pAtN = (double) relevantes/aux;
            double recallAtN = (double)relevantes/judgmentsMap.size();
            double mapAtN =(double) acumulador/judgmentsMap.size();
            //Revisar formula:
            //double mapAtN =(double) acumulador/cut;
            double mrr =(double) reverse/aux;

            String toSave= "query " + q.id() + " " + q.metadata().query();
            String[] toSave2 = {q.id(),String.valueOf(pAtN),String.valueOf(recallAtN), String.valueOf(mapAtN),String.valueOf(mrr)};
    		System.out.println(toSave);
    		writeToFile(path, toSave);
    		
    		toSave = " P@N: " + pAtN +" Recall@N: " + recallAtN + " MAP@N: " + mapAtN + " MRR: " + mrr;
    		System.out.println(toSave);
    		writeToFile(path, toSave);
    		writeToFile2(path2, toSave2);
    	
    		double[] data = { pAtN, recallAtN ,mapAtN , mrr};
    		return data;	      
	    }  
}