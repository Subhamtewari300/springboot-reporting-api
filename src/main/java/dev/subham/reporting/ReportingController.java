package dev.subham.reporting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ReportingController {
  private final JdbcTemplate db;
  public ReportingController(JdbcTemplate db){this.db=db;}

  public record NewTransaction(@NotBlank String category,@NotBlank String description,
      @NotNull @DecimalMin("0.00") BigDecimal amount,@NotNull LocalDate occurredOn){}
  public record TransactionRow(long id,String category,String description,BigDecimal amount,LocalDate occurredOn){}
  public record CategoryTotal(String category,long count,BigDecimal total){}
  public record Summary(long count,BigDecimal total,List<CategoryTotal> byCategory){}

  @PostMapping("/transactions") @ResponseStatus(HttpStatus.CREATED)
  public Map<String,String> create(@Valid @RequestBody NewTransaction body){
    db.update("insert into transactions(category,description,amount,occurred_on) values (?,?,?,?)",
      body.category(),body.description(),body.amount(),body.occurredOn());
    return Map.of("status","created");
  }

  @GetMapping("/reports/summary")
  public Summary summary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
    checkDates(from,to);
    List<CategoryTotal> cats=db.query("select category,count(*) cnt,coalesce(sum(amount),0) total from transactions where occurred_on between ? and ? group by category order by category",
       (rs,n)->new CategoryTotal(rs.getString("category"),rs.getLong("cnt"),rs.getBigDecimal("total")),from,to);
    long count=cats.stream().mapToLong(CategoryTotal::count).sum();
    BigDecimal total=cats.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO,BigDecimal::add);
    return new Summary(count,total,cats);
  }

  @GetMapping(value="/reports/export",produces="text/csv")
  public ResponseEntity<String> export(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
    checkDates(from,to);
    List<TransactionRow> items=db.query("select id,category,description,amount,occurred_on from transactions where occurred_on between ? and ? order by occurred_on,id",
      (rs,n)->new TransactionRow(rs.getLong("id"),rs.getString("category"),rs.getString("description"),rs.getBigDecimal("amount"),rs.getDate("occurred_on").toLocalDate()),from,to);
    StringBuilder csv=new StringBuilder("id,category,description,amount,occurred_on\r\n");
    for(TransactionRow t:items){
      csv.append(t.id()).append(',').append(escape(t.category())).append(',').append(escape(t.description()))
       .append(',').append(t.amount().toPlainString()).append(',').append(t.occurredOn()).append("\r\n");
    }
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transaction-report.csv")
        .body(csv.toString());
  }

  private static void checkDates(LocalDate from,LocalDate to){
    if(from.isAfter(to))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"from must be <= to");
  }

  static String escape(String value){
    // Quote/escape unsafe characters; prefix spreadsheet formulas to mitigate CSV injection.
    String safe = value;
    if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
    if(safe.contains(",")||safe.contains("\"")||safe.contains("\n")||safe.contains("\r"))
      return "\""+safe.replace("\"","\"\"")+"\"";
    return safe;
  }
}
