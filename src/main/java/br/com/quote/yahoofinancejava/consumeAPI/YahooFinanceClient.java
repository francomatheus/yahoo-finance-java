package br.com.quote.yahoofinancejava.consumeAPI;

import br.com.quote.yahoofinancejava.exceptions.ConsumeDataFromYhFinanceException;
import br.com.quote.yahoofinancejava.exceptions.DateFormatInvalidException;
import br.com.quote.yahoofinancejava.model.QuoteStockResponse;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZoneIdKeyDeserializer;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

public class YahooFinanceClient {

    // private String PATH_URI_YHFinance = "https://query1.finance.yahoo.com/v7/finance/download/{codigo}?period1=1558128752&period2=1589751152&interval=1d&events=history";
    private String PATH_URI_YHFinance = "https://query1.finance.yahoo.com/v7/finance/download/{codigo}";

    public List<QuoteStockResponse> getQuoteYahoo(
            String codigo,
            String date1,
            String date2,
            String interval,
            String events
            ){

        long period1 = converterStringDataToLocalDate(date1);
        long period2 = converterStringDataToLocalDate(date2);

        URI uri = UriComponentsBuilder
                .fromUriString(PATH_URI_YHFinance)
                .queryParam("period1",period1)
                .queryParam("period2",period2)
                .queryParam("interval",interval)
                .queryParam("events",events)
                .build(codigo.toUpperCase());

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest build = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        try {
            HttpResponse<String> send = client.send(build, HttpResponse.BodyHandlers.ofString());

            List<QuoteStockResponse> quoteStockResponses = converterJsonToQuoteStock(send);

            return quoteStockResponses;

        }catch (Exception e){
            throw new ConsumeDataFromYhFinanceException(e.getMessage());
        }


    }

    private long converterStringDataToLocalDate(String date) {
        try{

            LocalDateTime dateTime = LocalDate.parse(date).atStartOfDay();
            ZoneId zoneId = ZoneId.systemDefault();
            ZoneId zoneUTC = ZoneId.of("America/Los_Angeles");
            long l = dateTime.atZone(zoneId).toEpochSecond();
            long l1 = dateTime.atZone(zoneUTC).toInstant().toEpochMilli();

            return l;
        }catch (Exception ex){
            throw new DateFormatInvalidException("Formato de data yyyy-MM-dd");
        }

    }

    private List<QuoteStockResponse> converterJsonToQuoteStock(HttpResponse<String> send) {
        String body[] = send.body().split("\n");
        List<QuoteStockResponse> quoteStock = new ArrayList<>();

        List<String> lineFromResponse = getLineFromResponse(body);

        for(int i = 0 ; i < lineFromResponse.size(); i++){
            if(i>0){
                String[] split = lineFromResponse.get(i).split(",");
                QuoteStockResponse quoteStockResponse = new QuoteStockResponse();

                quoteStockResponse.setDate(LocalDate.parse(split[0]));
                quoteStockResponse.setOpen(BigDecimal.valueOf(Double.parseDouble(split[1])));
                quoteStockResponse.setHigh(BigDecimal.valueOf(Double.parseDouble(split[2])));
                quoteStockResponse.setLow(BigDecimal.valueOf(Double.parseDouble(split[3])));
                quoteStockResponse.setClose(BigDecimal.valueOf(Double.parseDouble(split[4])));
                quoteStockResponse.setAdjClose(BigDecimal.valueOf(Double.parseDouble(split[5])));
                quoteStockResponse.setVolume(Long.parseLong(split[6]));

                quoteStock.add(quoteStockResponse);
            }

        }

        return quoteStock;
    }

    private List<String> getLineFromResponse(String[] body) {
        List<String> allQuotesValid = new ArrayList<>();
        for (String line : body){
            if (!line.contains("null")){
                allQuotesValid.add(line);
            }
        }
        return allQuotesValid;
    }

}
