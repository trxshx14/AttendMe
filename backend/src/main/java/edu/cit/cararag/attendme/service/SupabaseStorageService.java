package edu.cit.cararag.attendme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadProfilePicture(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        System.out.println("📤 Uploading to: " + uploadUrl);
        System.out.println("📤 Key starts with: " + (serviceKey != null ? serviceKey.substring(0, Math.min(20, serviceKey.length())) + "..." : "NULL"));
        System.out.println("📤 File: " + file.getSize() + " bytes, type: " + file.getContentType());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl, HttpMethod.POST, entity, String.class
            );
            System.out.println("✅ Supabase status: " + response.getStatusCode());
            System.out.println("✅ Supabase body: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
            }
            throw new RuntimeException("Supabase non-2xx: " + response.getStatusCode());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("❌ Supabase HTTP error: " + e.getStatusCode());
            System.err.println("❌ Supabase error body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase upload failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("❌ Supabase exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}