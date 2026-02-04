package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.ApiResponse;
import com.genepay.genepaypaymentservice.service.PlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
@Tag(name = "Platform Administration", description = "APIs for platform owner to view collected fees and statistics")
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping("/balance")
    @Operation(summary = "Get Platform Balance", description = "Get total platform fees collected and available balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformBalance() {
        log.info("Fetching platform balance");
        Map<String, Object> balance = platformService.getPlatformBalance();
        return ResponseEntity.ok(ApiResponse.success("Platform balance retrieved", balance));
    }

    @GetMapping("/fees/summary")
    @Operation(summary = "Get Fee Summary", description = "Get detailed summary of all platform fees collected")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeeSummary(
            @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
        log.info("Fetching fee summary from {} to {}", startDate, endDate);
        Map<String, Object> summary = platformService.getFeeSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Fee summary retrieved", summary));
    }

    @GetMapping("/transactions/statistics")
    @Operation(summary = "Get Platform Statistics", description = "Get overall platform transaction statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformStatistics(
            @Parameter(description = "Period: day, week, month, year, all") @RequestParam(defaultValue = "all") String period) {
        log.info("Fetching platform statistics for period: {}", period);
        Map<String, Object> stats = platformService.getPlatformStatistics(period);
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
    }
}
