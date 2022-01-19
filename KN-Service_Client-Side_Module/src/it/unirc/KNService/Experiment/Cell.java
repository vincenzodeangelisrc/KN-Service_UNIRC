package it.unirc.KNService.Experiment;

public class Cell {

private int number;
private int salt;
private boolean shifted;


public int getNumber() {
	return number;
}
public void setNumber(int number) {
	this.number = number;
}
public int getSalt() {
	return salt;
}
public void setSalt(int salt) {
	this.salt = salt;
}


public boolean isShifted() {
	return shifted;
}
public void setShifted(boolean shifted) {
	this.shifted = shifted;
}
public Cell(int number, int salt, boolean s) {
	super();
	this.number = number;
	this.salt = salt;
	this.shifted=s;
}
@Override
public String toString() {
	return "Cell [number=" + number + ", salt=" + salt + ", shifted=" + shifted + "]";
}
@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + number;
	result = prime * result + salt;
	result = prime * result + (shifted ? 1231 : 1237);
	return result;
}
@Override
public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	Cell other = (Cell) obj;
	if (number != other.number)
		return false;
	if (salt != other.salt)
		return false;
	if (shifted != other.shifted)
		return false;
	return true;
}


}
