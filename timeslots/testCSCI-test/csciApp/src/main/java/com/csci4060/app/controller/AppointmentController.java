package com.csci4060.app.controller;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csci4060.app.model.APIresponse;
import com.csci4060.app.model.User;
import com.csci4060.app.model.appointment.Appointment;
import com.csci4060.app.model.appointment.AppointmentDate;
import com.csci4060.app.model.appointment.AppointmentDummy;
import com.csci4060.app.model.appointment.AppointmentTime;
import com.csci4060.app.model.appointment.TimeSlotResponse;
import com.csci4060.app.model.appointment.TimeSlots;
import com.csci4060.app.services.AppointmentDateService;
import com.csci4060.app.services.AppointmentService;
import com.csci4060.app.services.AppointmentTimeService;
import com.csci4060.app.services.EmailSenderService;
import com.csci4060.app.services.TimeSlotsService;
import com.csci4060.app.services.UserService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/appointment", produces = "application/json")
public class AppointmentController {

	@Autowired
	UserService userService;

	@Autowired
	AppointmentService appointmentService;

	@Autowired
	AppointmentDateService appointmentDateService;

	@Autowired
	AppointmentTimeService appointmentTimeService;

	@Autowired
	TimeSlotsService timeSlotsService;
	
	@Autowired
	private EmailSenderService emailSenderService;

	@PostMapping(path = "/set", consumes = "application/json")
	@PreAuthorize("hasRole('PM') or hasRole('ADMIN')")
	public APIresponse setAppointment(@RequestBody AppointmentDummy appointmentDummy, HttpServletRequest request) {

		List<User> recepientList = new ArrayList<User>();

		List<String> recepientsEmailList = appointmentDummy.getRecepients();
		for (String each : recepientsEmailList) {
			User recepient = userService.findByEmail(each);
			recepientList.add(recepient);
		}

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		String creatorUsername = "";

		if (principal instanceof UserDetails) {
			creatorUsername = ((UserDetails) principal).getUsername();
		}

		User createdBy = userService.findByUsername(creatorUsername);

		Appointment appointment = new Appointment(appointmentDummy.getName(), appointmentDummy.getDescription(),
				appointmentDummy.getDates(), recepientList, createdBy);

		appointmentService.save(appointment);

		List<AppointmentDate> dates = appointment.getDates();

		for (AppointmentDate date : dates) {

			appointmentDateService.save(date);

			List<AppointmentTime> times = date.getTimes();

			for (AppointmentTime time : times) {

				appointmentTimeService.save(time);

				LocalTime sTime = LocalTime.parse(time.getStartTime(), DateTimeFormatter.ofPattern("hh:mma"));
				LocalTime eTime = LocalTime.parse(time.getEndTime(), DateTimeFormatter.ofPattern("hh:mma"));
				long elapsedMinutes = Math.abs(Duration.between(sTime, eTime).toMinutes());

				long maxAppointment = elapsedMinutes / time.getInterv();

				LocalTime slotStartTime = sTime;

				for (int i = 0; i < maxAppointment; i++) {
					LocalTime slotEndTime = slotStartTime.plusMinutes(time.getInterv());
					timeSlotsService.save(new TimeSlots(slotStartTime, slotEndTime, false, date, appointment));
					slotStartTime = slotEndTime;
				}

			}
		}

		SimpleMailMessage mailMessage = new SimpleMailMessage();
		
		String[] emails = recepientList.toArray(new String[recepientList.size()]);
		
		mailMessage.setTo(emails);
		mailMessage.setSubject("Appointment Information");
		mailMessage.setFrom("ulmautoemail@gmail.com");
		mailMessage.setText(
				"A faculty has set an appointment for you. Please log in to you ULM communication app and register for the appointment. "
				+ "Thank you!");

		emailSenderService.sendEmail(mailMessage);

		return new APIresponse(HttpStatus.CREATED.value(), "Appointment created successfully", null);
	}

	@GetMapping(path = "/all")
	@PreAuthorize("hasRole('USER') or hasRole('PM') or hasRole('ADMIN')")
	public APIresponse getAppointments() {

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		String username = "";

		if (principal instanceof UserDetails) {
			username = ((UserDetails) principal).getUsername();
		}

		User user = userService.findByUsername(username);

		List<Appointment> appointments = appointmentService.findAllByRecepients(user);

		return new APIresponse(HttpStatus.OK.value(), "All appointments successfully sent.", appointments);

	}

	@GetMapping(path = "/timeslots/{id}")
	@PreAuthorize("hasRole('USER') or hasRole('PM') or hasRole('ADMIN')")
	public APIresponse getSlots(@PathVariable("id") Long appointmentId) {

		Appointment appointment = appointmentService.findById(appointmentId);

		List<TimeSlots> slotsFromAppointment = timeSlotsService.findByAppointment(appointment);

		List<TimeSlotResponse> timeSlotResponses = new ArrayList<TimeSlotResponse>();

		for (TimeSlots slots : slotsFromAppointment) {
			if (!slots.isSelected()) {
				timeSlotResponses.add(new TimeSlotResponse(slots.getId(), slots.getStartTime(), slots.getEndTime(),
						slots.getDate().getDate()));
			}
		}

		return new APIresponse(HttpStatus.OK.value(), "Time slots successfully sent.", timeSlotResponses);
	}

	@PostMapping(path = "timeslots/postSlot", produces = "application/json")
	@PreAuthorize("hasRole('USER') or hasRole('PM') or hasRole('ADMIN')")
	public APIresponse postSlots(@RequestBody TimeSlotResponse timeSlotResponse) {
		TimeSlots slotToRemove = timeSlotsService.findById(timeSlotResponse.getId());
		slotToRemove.setSelected(true);
		timeSlotsService.save(slotToRemove);
		return new APIresponse(HttpStatus.GONE.value(), "User has selected the timeslot.", slotToRemove);
	}
}
