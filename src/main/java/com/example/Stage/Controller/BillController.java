package com.example.Stage.Controller;

import com.example.Stage.Model.Bill;
import com.example.Stage.Repository.BillRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies/{policyNumber}/bills")
@CrossOrigin(origins = "http://localhost:4200")
public class BillController {

    private final BillRepository billRepository;

    public BillController(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    @GetMapping
    public List<Bill> getBills(@PathVariable String policyNumber, @RequestParam(required = false) String status) {
        List<Bill> bills = billRepository.findByPolicyPolicyNumber(policyNumber);
        if (status != null) {
            return bills.stream().filter(b -> status.equals(b.getStatus())).toList();
        }
        return bills;
    }
}