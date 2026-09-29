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
            "Phân tích hiệu suất dứt điểm của Erling Haaland so với chỉ số xG mùa 2024/2025",
            "So sánh phong cách thi đấu giữa Erling Haaland và Kylian Mbappé",
            "Đánh giá năng lực sáng tạo và kiến tạo (xA) của Bukayo Saka",
            "Phân tích tầm ảnh hưởng của Kevin De Bruyne ở khâu dọn cỗ cho Man City",
            "Đánh giá khả năng phòng ngự và phân phối bóng của Rodri",
            "Đội bóng nào đang có hiệu suất xG áp đảo nhất?"
        );
        return ApiResponse.success(prompts, start, true);
    }
}
