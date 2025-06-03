package es.udc.fi.ri.practica;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judgments( @JsonProperty("query-id")int query, @JsonProperty("corpus-id") String corpus, int score) 
{
	
}
