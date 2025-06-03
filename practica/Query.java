package es.udc.fi.ri.practica;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Query(@JsonProperty("_id") String id,  String text, Metadata metadata) 
{
	 @JsonIgnoreProperties(ignoreUnknown = true)
	  record Metadata(String query, String narrative) {

		public String query() {
			return query;
		}

		public String narrative() {
			return narrative;
		}

		
		
	 }

}
