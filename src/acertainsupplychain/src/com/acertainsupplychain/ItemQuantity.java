package com.acertainsupplychain;

/**
 * Represents an item and an associated quantity.
 */
public final class ItemQuantity {

	/**
	 * The ID of the item requested.
	 */
	private final int itemId;

	/**
	 * The number of items requested.
	 */
	private final int quantity;

	/**
	 * Creates an ItemQuantity instance with given item ID and quantity.
	 */
	public ItemQuantity(int itemId, int quantity) {
		this.itemId = itemId;
		this.quantity = quantity;
	}

	/**
	 * @return the itemId
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * @return the quantity
	 */
	public int getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "ItemQuantity: [" + itemId + "," + quantity + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ItemQuantity))
			return false;

		ItemQuantity item = (ItemQuantity) obj;
		return itemId == item.itemId && quantity == item.quantity;
	}

}
