package org.compassnavi;

import java.util.LinkedList;
import org.compassnavi.R;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class AzimuthSmoothingHelper 
{

	class SinusCosinus
	{
		public double sin;
		public double cos;

		public SinusCosinus(double sinus, double cosinus)
		{
			this.sin = sinus;
			this.cos = cosinus;
		}
	}
	
	private LinkedList<SinusCosinus> mValues = new LinkedList<SinusCosinus>();
	private int MaxAzimuthValues = 3;
	
	/**
	 * 
	 */
	public AzimuthSmoothingHelper()
	{
	}

	/**
	 * 
	 * @param degrees
	 * @return
	 */
	public float add(float degrees)
	{
		// Get sinus and cosinus values
		final double radians = Math.toRadians(degrees);
		final double sinus = Math.sin(radians);
		final double cosinus = Math.cos(radians);

		// Limit the queue to X values
		if (this.mValues.size() == MaxAzimuthValues)
			this.mValues.removeFirst();

		// Append value at the end of the queue
		this.mValues.addLast(new SinusCosinus(sinus, cosinus));

		return this.getWeightedAverage();
	}

	/**
	 * 
	 * @return
	 */
	public float getWeightedAverage()
	{
		float sumOfWeights = 0;
		
		final int size = this.mValues.size();
		float counter = 1;
		
		double sumSinus = 0;
		double sumCosinus = 0;
		for (SinusCosinus value : this.mValues)
		{
			// final float weight = 1;
			final float weight = counter / size;
			
			sumSinus += value.sin * weight;
			sumCosinus += value.cos * weight;
			sumOfWeights += weight;
			counter++;
		}

		// Calculate weighted average
		final double weightedAverageSinus = sumSinus / sumOfWeights;
		final double weightedAverageCosinus = sumCosinus / sumOfWeights;

		// Convert value back to degrees
		double degrees;
		if (weightedAverageSinus >= 0)
			degrees = Math.toDegrees(Math.acos(weightedAverageCosinus));
		else
			degrees = 180 + (180 - Math.toDegrees(Math.acos(weightedAverageCosinus)));
		
		return (float) degrees;
	}
	
	/**
	 * 
	 * @param maxSize
	 */
	public void setMaximumSize(int maxSize)
	{
		this.MaxAzimuthValues = maxSize;
	}
	
	/**
	 * 
	 */
	public void clear()
	{
		this.mValues.clear();
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{
		final AzimuthSmoothingHelper ash = new AzimuthSmoothingHelper();

		ash.setMaximumSize(1);

		System.out.println(String.format("%.2f Result: %.2f", 0.0, ash.add(0)));
		System.out.println(String.format("%.2f Result: %.2f", 90.0, ash.add(90)));
		System.out.println(String.format("%.2f Result: %.2f", 180.0, ash.add(180)));
		System.out.println(String.format("%.2f Result: %.2f", 270.0, ash.add(270)));
		System.out.println(String.format("%.2f Result: %.2f", 360.0, ash.add(3600)));

		System.out.println();
		
		ash.setMaximumSize(3);
		ash.clear();

		System.out.println(String.format("%.2f Result: %.2f", 350.0, ash.add(350)));
		System.out.println(String.format("%.2f Result: %.2f", 359.0, ash.add(359)));
		System.out.println(String.format("%.2f Result: %.2f", 2.0, ash.add(2)));
		System.out.println(String.format("%.2f Result: %.2f", 4.0, ash.add(4)));
		System.out.println(String.format("%.2f Result: %.2f", 5.0, ash.add(5)));

		ash.clear();

		System.out.println(String.format("%.2f Result: %.2f", 350.0, ash.add(350)));
		System.out.println(String.format("%.2f Result: %.2f", 15.0, ash.add(359)));
		System.out.println(String.format("%.2f Result: %.2f", 3.0, ash.add(2)));
	}

}
