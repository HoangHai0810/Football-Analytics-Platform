package com.football.analytics.controller;

import com.football.analytics.dto.AiAnalysisRequest;
import com.football.analytics.dto.AiAnalysisResponse;
import com.football.analytics.dto.ApiResponse;
import com.football.analytics.service.AiAnalystService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAnalystController {
    private final AiAnalystService aiAnalystService;

    public AiAnalystController(AiAnalystService aiAnalystService) {
        this.aiAnalystService = aiAnalystService;
    }

    @PostMapping("/analyze")
    public ApiResponse<AiAnalysisResponse> analyze(@RequestBody AiAnalysisRequest request) {
        long start = System.currentTimeMillis();
        AiAnalysisResponse data = aiAnalystService.analyze(request);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/prompts")
    public ApiResponse<List<String>> getSuggestedPrompts() {
        long start = System.currentTimeMillis();
        List<String> prompts = List.of(
            "Phân tích một cầu thủ đã có trong kho dữ liệu",
            "So sánh hai cầu thủ bằng thống kê mùa giải",
            "Cho tôi goals, assists và xG của một tiền đạo",
            "Kiểm tra hệ thống đã kết nối PostgreSQL chưa?"
        );
        return ApiResponse.success(prompts, start, true);
    }
}
