package id.co.bca.crosbatch.api;

import id.co.bca.crosbatch.batch.CrosJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST API to run and monitor the CROS batch (API mode only).
 *
 * <pre>
 * POST /api/cros-batch/executions                 start the job        body: {"period":"112025"} (optional)
 * GET  /api/cros-batch/executions/{id}            status + per-step counts
 * GET  /api/cros-batch/executions?limit=20        latest executions
 * POST /api/cros-batch/executions/{id}/recover    mark an execution left running by a killed process as FAILED
 * </pre>
 */
@RestController
@ConditionalOnWebApplication
@RequestMapping("/api/cros-batch/executions")
@Tag(name = "CROS batch", description = "Start and monitor the monthly CROS load")
@ApiResponse(responseCode = "401", description = "Missing or wrong API key",
        content = @Content(schema = @Schema(implementation = CrosBatchController.ErrorBody.class)))
public class CrosBatchController {

    private final CrosJobService jobService;

    public CrosBatchController(CrosJobService jobService) {
        this.jobService = jobService;
    }

    @Schema(description = "Start request. Omit the body or the period to load the previous month.")
    public record StartRequest(
            @Schema(description = "Period in MMyyyy", example = "112025") String period) {
    }

    @Schema(description = "Error response")
    public record ErrorBody(@Schema(example = "period '13-2025' does not match format MMyyyy") String error) {
    }

    @PostMapping
    @Operation(summary = "Start the job",
            description = "Starts the job in the background and returns at once. Poll the Location URL "
                    + "(GET /executions/{id}) until the status is COMPLETED or FAILED.")
    @ApiResponse(responseCode = "202", description = "Job started")
    @ApiResponse(responseCode = "400", description = "Invalid period or input folder missing",
            content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    @ApiResponse(responseCode = "409", description = "Another run is still in progress",
            content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    public ResponseEntity<ExecutionResponse> start(@RequestBody(required = false) StartRequest request)
            throws Exception {
        String period = request == null ? null : request.period();
        JobExecution execution = jobService.start(period, false);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(execution.getId()).toUri();
        return ResponseEntity.accepted().location(location).body(ExecutionResponse.from(execution));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one execution", description = "Status, per-step counts and failure messages.")
    @ApiResponse(responseCode = "200", description = "Execution found")
    @ApiResponse(responseCode = "404", description = "Unknown execution id",
            content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    public ExecutionResponse get(@Parameter(description = "Job execution id") @PathVariable long id) {
        return ExecutionResponse.from(jobService.find(id));
    }

    @GetMapping
    @Operation(summary = "List latest executions", description = "Newest first.")
    @ApiResponse(responseCode = "200", description = "Executions found")
    public List<ExecutionResponse> list(
            @Parameter(description = "Maximum number of executions (capped by crosbatch.api.max-list-size)")
            @RequestParam(defaultValue = "20") int limit) {
        return jobService.recent(limit).stream().map(ExecutionResponse::from).toList();
    }

    @PostMapping("/{id}/recover")
    @Operation(summary = "Recover a stale execution",
            description = "Marks an execution left STARTED by a killed process as FAILED, so a new run can start.")
    @ApiResponse(responseCode = "200", description = "Execution marked FAILED")
    @ApiResponse(responseCode = "400", description = "Execution is not running",
            content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    @ApiResponse(responseCode = "404", description = "Unknown execution id",
            content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    @ApiResponse(responseCode = "409", description = "Execution is alive in this process",
            content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    public ExecutionResponse recover(@Parameter(description = "Job execution id") @PathVariable long id) {
        return ExecutionResponse.from(jobService.recover(id));
    }
}
