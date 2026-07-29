package com.example.Stage.Controller;

import com.example.Stage.Model.Bill;
import com.example.Stage.Repository.BillRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "http://localhost:4200")
public class GlobalBillController {

    private final BillRepository billRepository;
    public GlobalBillController(BillRepository billRepository) { this.billRepository = billRepository; }

    @GetMapping
    public List<Bill> getAllBills() { return billRepository.findAll(); }
}