package com.vipha.cms.feature.appointment;

import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createNewAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        appointmentService.createAppointment(request);
    }


   @GetMapping("/{id}")
    public AppointmentResponse getAppointmentById(@PathVariable UUID id){
        return appointmentService.getAppointmentById(id);

   }
    @GetMapping
    public Page<AppointmentResponse> getMyAppointments(@RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                       @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return appointmentService.getMyAppointments(pageNumber, pageSize);
    }
   @GetMapping("/me")
   public List<AppointmentResponse> getMyAppointmentsList(){
        return appointmentService.getMyAppointmentsList();
   }

   @PutMapping("/{id}")
   @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAppointment(@PathVariable UUID id){
        appointmentService.cancelAppointment(id);
   }

   @PatchMapping("/{id}/me")
   @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAppointment(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentRequest request){
        appointmentService.updateAppointment(id, request);
   }


}
