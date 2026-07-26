package co.za.carrental.controller;

import co.za.carrental.api.dto.BookingRequest;
import co.za.carrental.api.dto.BookingResponse;
import co.za.carrental.domain.Booking;
import co.za.carrental.domain.BookingStatus;
import co.za.carrental.domain.Customer;
import co.za.carrental.service.IBookingService;
import co.za.carrental.service.ICustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final IBookingService bookingService;
    private final ICustomerService customerService;

    public BookingController(IBookingService bookingService, ICustomerService customerService) {
        this.bookingService = bookingService;
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@RequestBody BookingRequest request) {
        Booking created = bookingService.createFromRequest(request);

        BookingResponse dto = new BookingResponse(
                created.getBookingId(),
                created.getStartDate() != null ? created.getStartDate().toString() : null,
                created.getEndDate() != null ? created.getEndDate().toString() : null,
                created.getTotalCost(),
                created.getStatus() != null ? created.getStatus().name() : null
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> read(@PathVariable String bookingId, Authentication authentication) {
        String userEmail = authentication.getName();
        Customer customer = customerService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Booking> bookingOpt = bookingService.read(bookingId);

        if (bookingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking booking = bookingOpt.get();


        if (!"ADMIN".equals(customer.getRole()) &&
                !booking.getCustomer().getEmail().equals(userEmail)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<Booking> update(@PathVariable String bookingId,
                                          @RequestBody Booking incoming,
                                          Authentication authentication) {
        String userEmail = authentication.getName();
        Customer customer = customerService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (incoming.getBookingId() == null) {
            incoming.setBookingId(bookingId);
        } else if (!bookingId.equals(incoming.getBookingId())) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Booking> existingOpt = bookingService.read(bookingId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking existing = existingOpt.get();


        if (!"ADMIN".equals(customer.getRole()) &&
                !existing.getCustomer().getEmail().equals(userEmail)) {
            return ResponseEntity.status(403).build();
        }

        Booking saved = bookingService.update(incoming);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> delete(@PathVariable String bookingId, Authentication authentication) {
        String userEmail = authentication.getName();
        Customer customer = customerService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Booking> bookingOpt = bookingService.read(bookingId);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking booking = bookingOpt.get();


        if (!"ADMIN".equals(customer.getRole()) &&
                !booking.getCustomer().getEmail().equals(userEmail)) {
            return ResponseEntity.status(403).build();
        }

        bookingService.delete(bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String bookingId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Customer customer = customerService.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Booking> bookingOpt = bookingService.read(bookingId);

            if (bookingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Booking booking = bookingOpt.get();


            if (!"ADMIN".equals(customer.getRole()) &&
                    !booking.getCustomer().getEmail().equals(userEmail)) {
                return ResponseEntity.status(403).body("You are not authorized to cancel this booking");
            }

            if (booking.getStatus() == BookingStatus.CANCELLED) {
                return ResponseEntity.badRequest().body("Booking is already cancelled");
            }

            if (booking.getStatus() == BookingStatus.COMPLETED) {
                return ResponseEntity.badRequest().body("Cannot cancel a completed booking");
            }

            booking.setStatus(BookingStatus.CANCELLED);
            Booking updatedBooking = bookingService.update(booking);

            return ResponseEntity.ok(new BookingResponse(
                    updatedBooking.getBookingId(),
                    updatedBooking.getStartDate() != null ? updatedBooking.getStartDate().toString() : null,
                    updatedBooking.getEndDate() != null ? updatedBooking.getEndDate().toString() : null,
                    updatedBooking.getTotalCost(),
                    updatedBooking.getStatus() != null ? updatedBooking.getStatus().name() : null
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error cancelling booking: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAll(Authentication authentication) {
        String userEmail = authentication.getName();
        Customer customer = customerService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if ("ADMIN".equals(customer.getRole())) {
            return ResponseEntity.ok(bookingService.getAll());
        }


        List<Booking> userBookings = bookingService.getAll().stream()
                .filter(b -> b.getCustomer() != null &&
                        b.getCustomer().getEmail().equals(userEmail))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userBookings);
    }
}