package to.etc.domui.component.input;

import javax.annotation.*;

/**
 * General notfication event, can be used for definiton of general purpose callbacks.
 *
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on 27 Oct 2011
 */
public interface INotifyEvent {

	/**
	 * Notify listener with reason and optional params.
	 * @param sender
	 * @param reason
	 * @param params
	 * @throws Exception
	 */
	void onNotify(@Nonnull Object sender, @Nullable String reason, @Nullable Object... params) throws Exception;

}
