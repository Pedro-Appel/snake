package uk.co.bithatch.snake.ui;

import java.net.URL;
import java.util.Objects;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class DeviceOverview extends AbstractDeviceController implements Listener {

	@FXML
	private Label deviceName;
	@FXML
	private Label deviceSerial;
	@FXML
	private Label deviceFirmware;
	@FXML
	private Label deviceStatus;
	@FXML
	private ImageView deviceImage;
	@FXML
	private ImageView effect;
	@FXML
	private Label brightness;
	@FXML
	private Label brightnessLabel;
	@FXML
	private Label charging;
	@FXML
	private Label battery;
	@FXML
	private Hyperlink macros;

	private Effect lastEffect;
	private FadeTransition chargingAnim;
	private FadeTransition lowAnim;

	@Override
	protected void onConfigure() throws Exception {
		chargingAnim = BatteryControl.createFader(charging);
		lowAnim = BatteryControl.createFader(battery);
	}

	protected void onSetDevice() {
		Device dev = getDevice();
		dev.addListener(this);
		deviceName.textProperty().set(dev.getName());
		deviceImage.setImage(new Image(dev.getImage(), true));
		deviceSerial.textProperty().set(dev.getSerial());
		deviceFirmware.textProperty().set(dev.getFirmware());
		macros.managedProperty().bind(macros.visibleProperty());
		macros.visibleProperty().set(dev.getCapabilities().contains(Capability.MACROS));
		effect.visibleProperty().set(dev.getCapabilities().contains(Capability.EFFECTS));
		brightness.visibleProperty().set(dev.getCapabilities().contains(Capability.BRIGHTNESS));
		brightnessLabel.visibleProperty().set(dev.getCapabilities().contains(Capability.BRIGHTNESS));
		battery.visibleProperty().set(dev.getCapabilities().contains(Capability.BATTERY));
		charging.visibleProperty().set(dev.getCapabilities().contains(Capability.BATTERY));
		updateFromDevice(dev);
	}

	private void updateFromDevice(Device dev) {
		if (!Objects.equals(lastEffect, dev.getEffect())) {
			lastEffect = dev.getEffect();
			URL effectImage = App.class.getResource("effects/" + getClass().getSimpleName().toLowerCase() + ".png");
			if (effectImage != null)
				effect.setImage(new Image(effectImage.toExternalForm()));
		}
		if (dev.getCapabilities().contains(Capability.BRIGHTNESS)) {
			brightness.textProperty().set(dev.getBrightness() + "%");
		}
		if (dev.getCapabilities().contains(Capability.BATTERY)) {
			int level = dev.getBattery();
			battery.textProperty().set(BatteryControl.getBatteryIcon(level));
			boolean c = dev.isCharging();
			charging.visibleProperty().set(c);
			if (c)
				chargingAnim.play();
			else
				chargingAnim.stop();
			BatteryControl.setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, battery, lowAnim);
			BatteryControl.setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, charging, null);
		}
	}

	@FXML
	void evtSelect(ActionEvent evt) {
		context.push(DeviceDetails.class, this, Direction.FROM_RIGHT);
	}

	@FXML
	void evtMacros(ActionEvent evt) {
		context.push(Macros.class, this, Direction.FROM_BOTTOM);
	}

	@Override
	protected void onCleanUp() {
		getDevice().removeListener(this);
		chargingAnim.stop();
		lowAnim.stop();
	}

	@Override
	public void changed(Device device, Region region) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> changed(device, region));
		else {
			if (region == null)
				updateFromDevice(device);
		}
	}
}