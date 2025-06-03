package es.udc.fi.ri.practica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CovidDocument( @JsonProperty("_id") String id, String title, String text, Metadata metadata) 
{
	 @JsonIgnoreProperties(ignoreUnknown = true)
	  record Metadata(String url, String pubmed_id) {

		public String url() {
			return url;
		}

		public String pubmed_id() {
			return pubmed_id;
		}
		}
}
