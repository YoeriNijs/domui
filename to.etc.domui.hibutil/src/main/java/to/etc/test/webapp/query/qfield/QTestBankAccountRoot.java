package to.etc.test.webapp.query.qfield;
import javax.annotation.*;

import to.etc.webapp.query.*;
/**
 * @author <a href="mailto:dennis.bekkering@itris.nl">Dennis Bekkering</a>
 */
@Generated(value = { "This is a generated file. It will be overwritten during compilation. It is therefore useless to make any modifications" })
public final class QTestBankAccountRoot extends QTestBankAccount<QTestBankAccountRoot> {

	QTestBankAccountRoot() {
		super(null, null, null);
	}

	public @Nonnull
	QCriteria<to.etc.test.webapp.query.qfield.TestBankAccount> getCriteria() throws Exception {
		validateGetCriteria();
		return m_criteria;
	}
}