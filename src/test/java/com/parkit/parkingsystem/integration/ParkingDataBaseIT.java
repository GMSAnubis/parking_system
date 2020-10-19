package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.parkit.parkingsystem.customexceptions.ParkingIsFullException;
import com.parkit.parkingsystem.customexceptions.RegIsAlreadyParkedException;
import com.parkit.parkingsystem.customexceptions.RegistrationLengthException;
import com.parkit.parkingsystem.dao.ParkingSpotDao;
import com.parkit.parkingsystem.dao.TicketDao;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

  private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
  private static ParkingSpotDao parkingSpotDAO;
  private static TicketDao ticketDAO;
  private static DataBasePrepareService dataBasePrepareService;

  @Mock
  private static InputReaderUtil inputReaderUtil;

  @BeforeAll
  private static void setUp() {
    parkingSpotDAO = new ParkingSpotDao();
    parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
    ticketDAO = new TicketDao();
    ticketDAO.dataBaseConfig = dataBaseTestConfig;
    dataBasePrepareService = new DataBasePrepareService();
  }

  @BeforeEach
  private void setUpPerTest() {
    when(inputReaderUtil.readSelection()).thenReturn(1);
    dataBasePrepareService.clearDataBaseEntries();

  }

  @AfterEach
  private void tearDown() {
    dataBasePrepareService.clearDataBaseEntries();
  }

  @Test
  public void test_Registration_can_be_only_present_one_time_if_active_isAlreadyIn()
      throws RegistrationLengthException, RegIsAlreadyParkedException,
      ParkingIsFullException {
    when(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber()))
            .thenReturn("REG1TIME");
    ParkingService parkingService;
    parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO,
        ticketDAO);
    parkingService.processIncomingVehicle();
    assertThrows(RegIsAlreadyParkedException.class,
        () -> parkingService.processIncomingVehicle());
    Assertions.assertThat(ticketDAO.isKnownRegistrationInParking(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber())));
    assertTrue(ticketDAO.isKnownRegistrationInParking("REG1TIME"));

  }

  @Test
  public void test_Parking_A_Car() throws RegistrationLengthException {
    when(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber()))
            .thenReturn("IN_VEHICLE");
    ParkingService parkingService = new ParkingService(inputReaderUtil,
        parkingSpotDAO, ticketDAO);

    assertDoesNotThrow(() -> parkingService.processIncomingVehicle());

  }

  @Test
  public void test_Car_Parking_Lot_Exit() throws InterruptedException,
      RegistrationLengthException, ParkingIsFullException {
    /*
     * A pause is needed betwen incoming et exiting processes to avoid bug due
     * to the quick in out.
     */
    when(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber()))
            .thenReturn("EX_VEHICLE");
    ParkingService parkingService = new ParkingService(inputReaderUtil,
        parkingSpotDAO, ticketDAO);
    try {
      parkingService.processIncomingVehicle();
    } catch (RegIsAlreadyParkedException e) {
      e.printStackTrace();
    }
    Thread.sleep(500);

    assertDoesNotThrow(() -> parkingService.processExitingVehicle());
  }

  @Test
  public void test_welcome_for_recurrents_customers()
      throws InterruptedException, RegistrationLengthException,
      RegIsAlreadyParkedException, ParkingIsFullException {
    when(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber()))
            .thenReturn("RECURRING");
    ParkingService parkingService = new ParkingService(inputReaderUtil,
        parkingSpotDAO, ticketDAO);

    parkingService.processIncomingVehicle();
    Thread.sleep(500);
    parkingService.processExitingVehicle();
    parkingService.processIncomingVehicle();

    assertTrue(ticketDAO.isRecurringRegistration("RECURRING"));

  }

  @Test
  public void test_price_for_recurrents_customers()
      throws InterruptedException, RegistrationLengthException,
      RegIsAlreadyParkedException, ParkingIsFullException {
    when(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber()))
            .thenReturn("RECURRING2");
    ParkingService parkingService = new ParkingService(inputReaderUtil,
        parkingSpotDAO, ticketDAO);

    parkingService.processIncomingVehicle();

    Thread.sleep(500);
    parkingService.processExitingVehicle();

    assertTrue(ticketDAO.isRecurringRegistration("RECURRING2"));

  }

  @Test
  void test_no_reduced_price_for_non_recurrents_customers()
      throws InterruptedException, RegistrationLengthException,
      RegIsAlreadyParkedException, ParkingIsFullException {
    when(inputReaderUtil
        .controlVehicleRegistrationNumber(inputReaderUtil.getInputForVehicleRegNumber()))
            .thenReturn("RECURRING");
    ParkingService parkingService = new ParkingService(inputReaderUtil,
        parkingSpotDAO, ticketDAO);

    parkingService.processIncomingVehicle();

    assertFalse(ticketDAO.isRecurringRegistration("RECURRING"));

  }

}
