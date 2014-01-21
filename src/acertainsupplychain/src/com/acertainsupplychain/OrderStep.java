package com.acertainsupplychain;

import java.util.List;

/**
 * An OrderStep instance contains a quantity ordered against specific items, all
 * managed by a specific item supplier.
 */
public final class OrderStep {

	/**
	 * The ID of the item supplier that manages the items.
	 */
	private final int supplierId;

	/**
	 * The list of items ordered and their quantities.
	 */
	private final List<ItemQuantity> items;

	/**
	 * Constructs an OrderStep instance with given supplier, item, and quantity.
	 */
	public OrderStep(int supplierId, List<ItemQuantity> items) {
		this.supplierId = supplierId;
		this.items = items;
	}

	/**
	 * @return the supplierId
	 */
	public int getSupplierId() {
		return supplierId;
	}

	/**
	 * @return the items
	 */
	public List<ItemQuantity> getItems() {
		return items;
	}

	@Override
	public String toString() {
		return "OrderStep: [" + supplierId + "," + items + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof OrderStep))
			return false;

		OrderStep item = (OrderStep) obj;

		return supplierId == item.supplierId && items.equals(item.items);
	}

}
