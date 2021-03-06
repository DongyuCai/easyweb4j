package org.axe.interface_implement.type_convert;

import java.math.BigDecimal;

import org.axe.interface_.type_convert.BaseTypeConvert;

/**
 * java.math.BigDecimal=>java.lang.Double
 */
public final class BigDecimal2DoubleConvert implements BaseTypeConvert{

	@Override
	public Object convert(Object object,Object ...args) {
		return ((BigDecimal)object).doubleValue();
	}

}
