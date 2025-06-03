package es.udc.fi.ri.practica;

import java.io.IOException; //https://docs.oracle.com/javase/8/docs/api/java/io/IOException.html
import java.nio.file.FileSystemException;
import java.nio.file.Paths;//https://docs.oracle.com/javase/8/docs/api/java/nio/file/Paths.html
import java.util.ArrayList;
import java.util.List;//https://docs.oracle.com/javase/8/docs/api/java/util/List.html
import java.util.Random;
import java.util.concurrent.ExecutorService;//https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
import java.util.concurrent.Executors;//https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executors.html
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;//https://lucene.apache.org/core/7_0_1/core/org/apache/lucene/document/Field.html
//import org.apache.lucene.document.Field.TermVector//https://lucene.apache.org/core/5_4_1/core/org/apache/lucene/document/Field.TermVector.html
import org.apache.lucene.document.KeywordField;//https://lucene.apache.org/core/9_9_0/core/org/apache/lucene/document/KeywordField.html
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;//https://lucene.apache.org/core/9_9_0/core/org/apache/lucene/document/TextField.html
import org.apache.lucene.index.IndexWriter;//https://lucene.apache.org/core/7_0_1/core/org/apache/lucene/index/IndexWriter.html
import org.apache.lucene.index.IndexWriterConfig;//https://lucene.apache.org/core/7_0_1/core/org/apache/lucene/index/IndexWriterConfig.html
import org.apache.lucene.index.IndexWriterConfig.OpenMode;//https://lucene.apache.org/core/7_0_1/core/org/apache/lucene/index/IndexWriterConfig.OpenMode.html
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;//https://lucene.apache.org/core/8_0_0/core/org/apache/lucene/store/Directory.html
import org.apache.lucene.store.FSDirectory;//https://https://lucene.apache.org/core/8_0_0/core/org/apache/lucene/store/FSDirectory.html
import org.apache.lucene.store.LockObtainFailedException;
import com.fasterxml.jackson.databind.ObjectReader;
//https://lucene.apache.org/core/4_0_0/core/org/apache/lucene/document/Document.html
import com.fasterxml.jackson.databind.json.JsonMapper;


public class IndexTrecCovid  implements AutoCloseable {
	

/**
 * Main funcion principañl
 * @param args
 * @throws Exception
 */
public static void main(String[] args) throws Exception {

	List<CovidDocument> docu = null;		
				
	 //Variables necesarias podemos inicializarlas en el main y pasarlas como parametros si es necesario
	String index = null;
	String docs = null;
	String openMode= null;
	String iModel = null; //o esto o el bool de abajo que son dos tipos y requieren un float como parametro??
	float lambda = 0;
	float k1 = 0;
	int numThreads = Runtime.getRuntime().availableProcessors();
    String usage =
        "IndexTrecCovid"
            + " [-index INDEX_PATH] [-docs DOCS_PATH] [-openmode mode] [-indexingmodel model value] \n\n"
            + "This indexes the documents in DOCS_PATH, creating a Lucene index";
   
//comprobamos que tenemos los argumentos necesarios
    if (args.length <9) {
		System.out.println("A folder is needed for the index");
		  System.err.println("Usage: " + usage);
	      System.exit(1);
	}
    
			    
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-index":
          index = args[++i];
          break;
        case "-docs":
          docs = args[++i];
          break;
        case "-openmode":
           openMode = args[++i];
           if(openMode.equalsIgnoreCase("create"))
        	   numThreads=1;
          break;
        case "-indexingmodel":
        	iModel=args[++i];
          if (iModel.equals("jm")){
		  lambda = Float.valueOf(args[++i]);
		  }else {
		  k1 = Float.valueOf(args[++i]);
	      }
          break;
        default:
          throw new IllegalArgumentException("unknown parameter " + args[i]);
      }
    }

    if (index == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }
    
    if (docs == null) {
	      System.err.println("Usage: " + usage);
	      System.exit(1);
	      }
	    
    //generacion de pool de hilos
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);	    	    
    try {
		        //Para procesar en cada hilo
		        final String finalINDEX_PATH = index;
		        final String finalopenMode = openMode;
		        final String finaliMode = iModel;
		        final float finallambda = lambda;
		        final float finalK1 = k1;
		        //leemos de la carpeta donde estan los documentos a indexar
		        var is = IndexTrecCovid.class.getResourceAsStream(docs);
		        ObjectReader reader = JsonMapper.builder().findAndAddModules().build()
		                .readerFor(CovidDocument.class);
		        try {
		            docu = reader.<CovidDocument>readValues(is).readAll();
		        } catch (IOException e) {
		        	System.err.println("Usage: " + usage);
		            System.exit(1);
		        }   
		     // Dividimos los documentos segun los hilos con los que trabajemos
		        int blockSize = docu.size() / numThreads;
		        List<DocumentBlock> documentBlocks = new ArrayList<>();

		        for (int i = 0; i < numThreads; i++) {
		            int start = i * blockSize;
		            int end = (i == numThreads - 1) ? docu.size() : (i + 1) * blockSize;
		            List<CovidDocument> block = docu.subList(start, end);
		            documentBlocks.add(new DocumentBlock(block));
		        }
		        documentBlocks.forEach(block -> executorService.submit(() -> {
		            
		                insertToIndex(finalINDEX_PATH, block, finalopenMode, finaliMode, finallambda, finalK1);
		            
		        })); 
		        executorService.shutdown();   
		           
		            while (!executorService.isTerminated()) {
		                //Comprueba si los hilos han acabado 
		            	 if (!executorService.awaitTermination(70, TimeUnit.SECONDS)) {
			                    // Si el tiempo de espera expira, se puede imprimir un mensaje o tomar otra acción apropiada
			                    System.err.println("El tiempo de espera ha expirado mientras se esperaba la finalización de las tareas del pool de hilos.");	                
		            	 }		            		
		            }
		            executorService.shutdown();		          
		            System.out.println("Created index");        

		    } finally {    
		        System.exit(1);
		    }
    
}
    	
/**
 * Crea los documentos de indexado y realiza la indexacion	
 * @param INDEX_Path
 * @param block documentos que se revisaran
 * @param finalopenMode
 * @param finaliMode
 * @param finallambda
 * @param finalK1
 */
	 private static void insertToIndex(String INDEX_Path,DocumentBlock block,  String finalopenMode, 
			 	String finaliMode, float finallambda, float finalK1) {  

		 
		 try {
	         // Configurar el directorio de índice
	          Directory dir = FSDirectory.open(Paths.get(INDEX_Path));
	          // Configurar el analizador de consultas
	          IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
	          
	          if (finaliMode.equalsIgnoreCase("jm")) {//comprobamos si se crea o se modifica
	               iwc.setSimilarity(new LMJelinekMercerSimilarity(finallambda));
	             } else if(finaliMode.equalsIgnoreCase("bm25")) {  // Add new documents to an existing index:
	               iwc.setSimilarity(new BM25Similarity(finalK1,(float) 0.75));
	             } else {
	            	 System.err.println("Wrong similarity Mode");
			            System.exit(1);
	             }
	          
	             if (finalopenMode.equalsIgnoreCase("create")) {//comprobamos si se crea o se modifica
	               iwc.setOpenMode(OpenMode.CREATE);
	             } else if(finalopenMode.equalsIgnoreCase("create_or_append")) {  // Add new documents to an existing index:
	               iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
	             } else if (finalopenMode.equalsIgnoreCase("append")) {
	            	 iwc.setOpenMode(OpenMode.APPEND);
	             }else {
	            	 System.err.println("Wrong open Mode");
			            System.exit(1);
	             }  
	             
	  	        try (// Crear un escritor de índice
				IndexWriter writer = new IndexWriter(dir, iwc)) {
						//añadimos el archivo
		  	        	if (finalopenMode.equalsIgnoreCase("create")) {//comprobamos si se crea o se modifica
		  	        		create(block, writer);
		  	        	} else  {  // Add new documents to an existing index:
		  	        		create_add(block, writer);
		  	        	} 
	  	        writer.close(); 
	  	        	}
	  	        } catch(LockObtainFailedException e) {
	  	        	System.out.println("Retry indexing ");
	  	        	
			 try {
				 Thread.sleep(new Random().nextInt(3000));
				 insertToIndex(  INDEX_Path, block ,   finalopenMode, 
							 finaliMode, finallambda,  finalK1);
			} catch (InterruptedException e1) {
			 
				System.err.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
	            System.exit(1);
			 }
		} catch (FileSystemException exc){
			 insertToIndex(  INDEX_Path, block ,   finalopenMode, 
						 finaliMode, finallambda,  finalK1);
			
			
		} catch (Exception e) {
		 
	             System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());  
	             System.exit(1);
	           		  
	}
		       
}
	 

/**
 * Funcion que crea los documentos y actualiza el indice	 
 * @param wblock
 * @param writer
 * @throws IOException
 */	 
	 public static void create_add (DocumentBlock wblock, IndexWriter writer ) throws IOException {
		
		 wblock.getDocuments().forEach(block -> {
			 org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		       	doc.add(new KeywordField("id", block.id(), Field.Store.YES));
		       	doc.add(new TextField("title", block.title(), Field.Store.YES));
		       	doc.add(new TextField("text", block.text(), Field.Store.YES));
		       	doc.add(new StringField("url", block.metadata().url(), Field.Store.YES));
		       	doc.add(new StringField("pubmed_id", block.metadata().pubmed_id(), Field.Store.YES));
		       	try {
					writer.updateDocument((new org.apache.lucene.index.Term("path", doc.toString())), doc);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};});
	 }
  

/**
 * Funcion que crea los documentos y el indice	 
 * @param wblock
 * @param writer
 * @throws IOException
 */	 
	 public static void create (DocumentBlock wblock, IndexWriter writer ) throws IOException {
		 wblock.getDocuments().forEach(block -> {
			 org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		       	doc.add(new KeywordField("id", block.id(), Field.Store.YES));
		       	doc.add(new TextField("title", block.title(), Field.Store.YES));
		       	doc.add(new TextField("text", block.text(), Field.Store.YES));
		       	doc.add(new StringField("url", block.metadata().url(), Field.Store.YES));
		       	doc.add(new StringField("pubmed_id", block.metadata().pubmed_id(), Field.Store.YES));
		       	try {
					writer.addDocument(doc);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};});
	        	
	
		 
	 }
	 
		//funcion de cierre de los hilos
	 @Override
	 public void close() throws Exception {
			
	


	}
	
}
	 



