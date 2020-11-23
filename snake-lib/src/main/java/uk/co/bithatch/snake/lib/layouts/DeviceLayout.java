package uk.co.bithatch.snake.lib.layouts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region;

public class DeviceLayout implements uk.co.bithatch.snake.lib.layouts.DeviceView.Listener {

	public interface Listener {
		void layoutChanged(DeviceLayout layout, DeviceView view);

		void viewRemoved(DeviceLayout layout, DeviceView view);

		void viewChanged(DeviceLayout layout, DeviceView view);

		void viewElementAdded(DeviceLayout layout, DeviceView view, IO element);

		void viewElementChanged(DeviceLayout layout, DeviceView view, IO element);

		void viewElementRemoved(DeviceLayout layout, DeviceView view, IO element);

		void viewAdded(DeviceLayout layout, DeviceView view);
	}

	private Map<ViewPosition, DeviceView> views = Collections.synchronizedMap(new LinkedHashMap<>());
	private String name;
	private int matrixWidth;
	private int matrixHeight;
	private boolean readOnly;
	private Path base;
	private DeviceType deviceType = DeviceType.UNRECOGNISED;
	private List<Listener> listeners = new ArrayList<>();

	public DeviceLayout() {
	}

	public DeviceLayout(Device device) {
		this.name = device.getName();
		this.deviceType = device.getType();
		if(device.getCapabilities().contains(Capability.MATRIX)) {
			this.matrixHeight = device.getMatrixSize()[0];
			this.matrixWidth = device.getMatrixSize()[1];
		}
	}

	public DeviceLayout(Path archive, JsonObject sequenceJson) {
		setBase(archive);
		setName(sequenceJson.has("name") ? sequenceJson.get("name").getAsString() : null);
		setMatrixHeight(sequenceJson.get("matrixHeight").getAsInt());
		setMatrixWidth(sequenceJson.get("matrixWidth").getAsInt());
		setDeviceType(DeviceType.valueOf(sequenceJson.get("deviceType").getAsString()));
		JsonArray frames = sequenceJson.get("views").getAsJsonArray();
		for (JsonElement viewElement : frames) {

			JsonObject viewObject = viewElement.getAsJsonObject();

			/* View */
			DeviceView view = new DeviceView();
			view.setLayout(this);
			view.setDesaturateImage(
					viewObject.has("desaturateImage") ? viewObject.get("desaturateImage").getAsBoolean() : false);
			view.setImageOpacity(viewObject.has("imageOpacity") ? viewObject.get("imageOpacity").getAsFloat() : 1);
			view.setImageScale(viewObject.has("imageScale") ? viewObject.get("imageScale").getAsFloat() : 1);
			view.setImageUri(viewObject.has("imageUri") ? viewObject.get("imageUri").getAsString() : null);
			view.setPosition(ViewPosition.valueOf(viewObject.get("position").getAsString()));

			/* Elements */
			JsonArray elements = viewObject.get("elements").getAsJsonArray();
			for (JsonElement elementRow : elements) {

				JsonObject elementObject = elementRow.getAsJsonObject();
				ComponentType viewType = ComponentType.valueOf(elementObject.get("type").getAsString());
				IO element = null;
				switch (viewType) {
				case LED:
					LED led = new LED();
					led.setMatrixX(elementObject.has("matrixX") ? elementObject.get("matrixX").getAsInt() : 0);
					led.setMatrixY(elementObject.has("matrixY") ? elementObject.get("matrixY").getAsInt() : 0);
					element = led;
					break;
				case KEY:
					Key key = new Key();
					key.setMatrixX(elementObject.has("matrixX") ? elementObject.get("matrixX").getAsInt() : 0);
					key.setMatrixY(elementObject.has("matrixY") ? elementObject.get("matrixY").getAsInt() : 0);
					element = key;
					break;
				case MATRIX_CELL:
					MatrixCell matrixCell = new MatrixCell();
					matrixCell.setMatrixX(elementObject.has("matrixX") ? elementObject.get("matrixX").getAsInt() : 0);
					matrixCell.setMatrixY(elementObject.has("matrixY") ? elementObject.get("matrixY").getAsInt() : 0);
					matrixCell.setRegion(
							elementObject.has("region") ? Region.Name.valueOf(elementObject.get("region").getAsString())
									: null);
					element = matrixCell;
					break;
				case AREA:
					Area area = new Area();
					area = new Area();
					area.setRegion(
							elementObject.has("region") ? Region.Name.valueOf(elementObject.get("region").getAsString())
									: null);
					element = area;
					break;
				default:
					throw new UnsupportedOperationException();
				}

				element.setLabel(elementObject.has("label") ? elementObject.get("label").getAsString() : null);
				element.setX(elementObject.has("x") ? elementObject.get("x").getAsFloat() : 0);
				element.setY(elementObject.has("y") ? elementObject.get("y").getAsFloat() : 0);
				view.addElement(element);
			}
			addView(view);
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.add(listener);
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
		fireChanged(null);
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		fireChanged(null);
	}

	public Map<ViewPosition, DeviceView> getViews() {
		return views;
	}

	public void addView(DeviceView view) {
		synchronized (views) {
			view.setLayout(this);
			view.addListener(this);
			views.put(view.getPosition(), view);
		}
		fireViewAdded(view);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		fireChanged(null);
	}

	public int getMatrixWidth() {
		return matrixWidth;
	}

	public void setMatrixWidth(int matrixWidth) {
		this.matrixWidth = matrixWidth;
		fireChanged(null);
	}

	public int getMatrixHeight() {
		return matrixHeight;
	}

	public void setMatrixHeight(int matrixHeight) {
		this.matrixHeight = matrixHeight;
		fireChanged(null);
	}

	public void removeView(ViewPosition position) {
		DeviceView view = views.remove(position);
		if (view != null) {
			view.removeListener(this);
			fireViewRemoved(view);
		}
	}

	public void setBase(Path base) {
		this.base = base;
		fireChanged(null);
	}

	public Path getBase() {
		return base;
	}

	void fireChanged(DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).layoutChanged(this, view);
	}

	void fireViewAdded(DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewAdded(this, view);
	}

	void fireViewChanged(DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewChanged(this, view);
	}

	void fireViewElementAdded(DeviceView view, IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewElementAdded(this, view, element);
	}

	void fireViewElementChanged(DeviceView view, IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewElementChanged(this, view, element);
	}

	void fireViewElementRemoved(DeviceView view, IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewElementRemoved(this, view, element);
	}

	void fireViewRemoved(DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewRemoved(this, view);
	}

	@Override
	public void viewChanged(DeviceView view) {
		fireViewChanged(view);
	}

	@Override
	public String toString() {
		return "DeviceLayout [views=" + views + ", name=" + name + ", matrixWidth=" + matrixWidth + ", matrixHeight="
				+ matrixHeight + ", readOnly=" + readOnly + ", base=" + base + ", deviceType=" + deviceType + "]";
	}

	@Override
	public void elementAdded(DeviceView view, IO element) {
		fireViewElementAdded(view, element);
	}

	@Override
	public void elementRemoved(DeviceView view, IO element) {
		fireViewElementRemoved(view, element);
	}

	@Override
	public void elementChanged(DeviceView view, IO element) {
		fireViewElementChanged(view, element);
	}

	public void setViews(List<DeviceView> newViews) {
		synchronized (views) {
			views.clear();
			for (DeviceView v : newViews)
				views.put(v.getPosition(), v);
		}
		fireChanged(null);
	}

	public DeviceView getViewThatHas(ComponentType area) {
		synchronized (views) {
			for (Map.Entry<ViewPosition, DeviceView> en : views.entrySet()) {
				for (IO el : en.getValue().getElements()) {
					if (ComponentType.fromClass(el.getClass()) == area) {
						return en.getValue();
					}
				}
			}
		}
		return null;
	}

	public List<DeviceView> getViewsThatHave(ComponentType area) {
		List<DeviceView> v = new ArrayList<>();
		synchronized (views) {
			for (Map.Entry<ViewPosition, DeviceView> en : views.entrySet()) {
				for (IO el : en.getValue().getElements()) {
					if (ComponentType.fromClass(el.getClass()) == area) {
						v.add(en.getValue());
						break;
					}
				}
			}
		}
		return v;
	}

	void updatePosition(DeviceView deviceView) {
		synchronized (views) {
			Map<ViewPosition, DeviceView> m = new LinkedHashMap<>();
			for (DeviceView v : views.values()) {
				m.put(v.getPosition(), v);
			}
			views.clear();
			views.putAll(m);
		}
	}
}