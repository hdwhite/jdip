package info.jdip.world.variant.data;

public class Influence
{
	private String provinceName = null;
	private String powerName = null;	
	private String ownerName = null;	

	/** Get the province name. */
	public String getProvinceName() {
        return provinceName;
    }

	/** Set the province name. */
	public void setProvinceName(String value) {
        provinceName = value;
    }

	/** Get the name of the Power that owns this supply center. */
	public String getPowerName() {
        return powerName;
    }

	/** Set the name of the Power that owns this supply center. 
		"none" is acceptable, but "any" is not.
	*/
	public void setPowerName(String value) 		
	{ 
		if("any".equalsIgnoreCase(value))
		{
			throw new IllegalArgumentException();
		}

		powerName = value; 
	}// setOwnerName()

	/** For debugging only! */
	public String toString()
	{
		StringBuilder sb = new StringBuilder(256);
		sb.append(this.getClass().getName());
		sb.append('[');
		sb.append("provinceName=");
		sb.append(provinceName);
		sb.append(",powerName=");
		sb.append(powerName);
		sb.append(",ownerName=");
		sb.append(ownerName);
		sb.append(']');
		return sb.toString();
	}// toString()
}// nested class SupplyCenter