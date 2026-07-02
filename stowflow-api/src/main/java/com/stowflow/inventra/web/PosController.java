package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.PosDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.service.PosService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;
    private final TenantService tenantService;

    @PostMapping("/sale")
    @PreAuthorize(InventraPolicies.POS_SALE)
    public PosDtos.PosSaleResponse sale(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication,
            @Valid @RequestBody PosDtos.PosSaleRequest body
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return posService.recordSale(tenantService.requireBySlug(tenantSlug), body, u.getUsername());
    }

    @GetMapping("/sales")
    @PreAuthorize(InventraPolicies.POS_SALES_READ)
    public PosDtos.PagedSales listSales(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String soldBy
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        var tenant = tenantService.requireBySlug(tenantSlug);
        var p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PosDtos.SaleListItem> pg = posService.listSales(tenant, p, soldBy, u);
        return new PosDtos.PagedSales(
                pg.getContent(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize());
    }

    @GetMapping("/sales/commercials")
    @PreAuthorize(InventraPolicies.POS_COMMERCIAL_DIRECTORY)
    public List<PosDtos.SellerOption> listCommercials(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug
    ) {
        return posService.listCommercialSellerOptions(tenantService.requireBySlug(tenantSlug));
    }

    @GetMapping("/sales/{id}")
    @PreAuthorize(InventraPolicies.POS_SALES_READ)
    public PosDtos.PosSaleResponse getSale(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication,
            @PathVariable("id") Long id
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return posService.getSale(tenantService.requireBySlug(tenantSlug), id, u);
    }

    @GetMapping(value = "/sales/{id}/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(InventraPolicies.POS_SALES_READ)
    public ResponseEntity<byte[]> invoicePdf(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication,
            @PathVariable("id") Long id
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        byte[] pdf = posService.invoicePdf(tenantService.requireBySlug(tenantSlug), id, u);
        ContentDisposition cd = ContentDisposition.attachment()
                .filename("facture-" + id + ".pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
