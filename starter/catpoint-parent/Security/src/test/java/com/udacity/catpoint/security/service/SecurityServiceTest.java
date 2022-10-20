package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private StatusListener statusListener;
    @Mock
    Sensor sensor;

    @Mock
    ImageService imageService;

    @Mock
    SecurityRepository securityRepository;

    @InjectMocks
    SecurityService securityService;

    @BeforeEach
    public void setupEach() {
        MockitoAnnotations.openMocks(this);
    }


    @ParameterizedTest // Test No 1
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_AWAY", "ARMED_HOME"})
    public void when_alarmArmed_and_sensorActivated_then_putSystem_into_pendingAlarmStatus(ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
       // when(sensor.getActive()).thenReturn(false);
        when(sensor.getActive()).thenReturn(true);
        boolean activeFlag = true;

        securityService.changeSensorActivationStatus(sensor, activeFlag);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(sensor, times(1)).setActive(any(Boolean.TYPE));
        verify(sensor, times(2)).getActive();
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
    }


    //If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest // Test No 2
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_AWAY", "ARMED_HOME"})
    public void when_alarmArmed_and_sensorActivated_and_systemInPendingAlarm_then_putAlarmStatus_to_alarm(ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor.getActive()).thenReturn(true);
        boolean activeFlag = true;

        securityService.changeSensorActivationStatus(sensor, activeFlag);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(sensor, times(1)).setActive(any(Boolean.TYPE));
        verify(sensor, times(2)).getActive();
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
    }

    //If pending alarm and all sensors are inactive, return to no alarm state.
    @Test //Test No 3
    public void when_pendingAlarmStatus_and_all_sensors_inactive_then_set_alarmState_to_noAlarmStatus(){
        when(sensor.getActive()).thenReturn(true);
        boolean systemFlag = false;
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, systemFlag);

        verify(sensor, times(1)).setActive(any(Boolean.TYPE));
        verify(sensor, times(3)).getActive();
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest //Test No 4
    @ValueSource(booleans = {true, false})
    public void when_alarmIsActive_changeSensorState_shouldNotAffectAlarmState(boolean sensorState){
       // when(sensor.getActive()).thenReturn(sensorState);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, !sensorState);

        verify(sensor, times(1)).setActive(any(Boolean.TYPE));
        verify(sensor, never()).getActive();
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test //Test No 5
    public void when_sensorActivated_and_systemAlreadyActive_and_alarmInPendingState_then_changeAlarm_to_alarmState(){
        when(sensor.getActive()).thenReturn(true);
        boolean systemFlag = true;
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, systemFlag);

        verify(sensor, times(1)).setActive(any(Boolean.TYPE));
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(sensor, times(2)).getActive();
    }

    //If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test //Test No 6
    public void when_sensorIsDeactivated_and_systemInactive_then_make_noChange_to_alarmState(){
        when(sensor.getActive()).thenReturn(false);
        boolean systemFlag = false;

        securityService.changeSensorActivationStatus(sensor, systemFlag);

        verify(sensor, times(1)).setActive(any(Boolean.TYPE));
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test //Test No 7
    public void when_imageService_find_a_cat_and_system_isArmedHome_then_put_alarmStatus_to_alarmState(){
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test //Test No 8
    public void when_imageService_find_no_cat_and_sensors_areNotActive_then_change_alarm_to_noAlarmStatus(){
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If the system is disarmed, set the status to no alarm.
    @Test // Test No 9
    public void when_systemIsDisarmed_then_set_alarmStatus_to_noAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
    }

    //    If the system is armed, reset all sensors to inactive.
    @ParameterizedTest // Test No 10
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_AWAY", "ARMED_HOME"})
    public void when_systemIsArmed_then_reset_allSensors_to_inActive(ArmingStatus armingStatus){
        Set<Sensor> activeSensors = createSensors(true, 8);
        when(securityRepository.getSensors()).thenReturn(activeSensors);

        securityService.setArmingStatus(armingStatus);
        verify(securityRepository, times(1)).setArmingStatus(armingStatus);
        verify(securityRepository, times(1)).getSensors();
        assertAllSensorsMatchInputActiveState(activeSensors, false);
    }

    //If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test //Test No 11
    public void when_systemIsArmedHome_and_imageService_findCat_then_set_alarmStatus_to_alarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    private void assertAllSensorsMatchInputActiveState(Set<Sensor> sensors, boolean sensorActiveStatus) {
        sensors.forEach(sensor1 -> assertEquals(sensorActiveStatus, sensor1.getActive()));
    }

    private Set<Sensor> createSensors(boolean sensorActiveState, int numberOfSensors) {
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0; i < numberOfSensors; i++){
            Sensor newSensor = new Sensor(getRandomString(), SensorType.randomSensorType());
            newSensor.setActive(sensorActiveState);
            sensors.add(newSensor);
        }
        return sensors;
    }

    private String getRandomString(){
        return UUID.randomUUID().toString();
    }

    //Tests for code coverage here:


    @Test
    void test_add_remove_sensor(){
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
        securityService.getSensors();
    }

    @Test
    void test_getAlarm_status(){
        securityService.getAlarmStatus();
    }

    @Test
    void test_add_remove_status_listeners(){
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    /*
     * The following unit tests are used to increase the unit test code coverage
     */
    @Test
    public void test_add_and_remove_statusListener_method_for_test_code_coverage() {
        StatusListener mockStatusListener = mock(StatusListener.class);
        securityService.addStatusListener(mockStatusListener);
        securityService.removeStatusListener(mockStatusListener);
    }

    @Test
    public void test_add_sensor_method_for_test_code_coverage() {
        Sensor mockSensor = mock(Sensor.class);
        securityService.addSensor(mockSensor);
        securityService.removeSensor(mockSensor);
        verify(securityRepository, times(1)).addSensor(any(Sensor.class));
        verify(securityRepository, times(1)).removeSensor(any(Sensor.class));
    }

    @Test
    public void test_getAlarmStatus_for_test_code_coverage() {
        securityService.getAlarmStatus();
        verify(securityRepository, times(1)).getAlarmStatus();
    }

    @Test
    public void when_sensorIsActivated_and_alarmStatus_is_alarm_then_set_alarmStatus_to_pendingStatus() {
        when(sensor.getActive()).thenReturn(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor);

        verify(sensor, times(1)).getActive();
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void when_sensorIs_notActivated_and_armStatus_is_disArmed_then_do_nothing() {
        when(sensor.getActive()).thenReturn(false);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.changeSensorActivationStatus(sensor);

        verify(sensor, times(1)).getActive();
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void test_securityService_constructor() {
        SecurityService securityService1 = new SecurityService(securityRepository);
    }

    @ParameterizedTest   //
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void when_system_armed_set_sensor_to_deactivated(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        securityService.changeSensorActivationStatus(sensor);

        verify(sensor, times(1)).setActive(false);
        verify(securityRepository, times(1)).updateSensor(any(Sensor.class));
    }

    @Test
    public void when_sensor_deactivated_and_alarmStatus_pending_then_set_alarmStatus_to_noAlarm() {
        when(sensor.getActive()).thenReturn(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor);

        verify(sensor, times(1)).getActive();
        verify(securityRepository, times(2)).getAlarmStatus();
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
}
