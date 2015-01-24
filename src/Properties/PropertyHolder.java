/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Properties;

import java.util.Map;

/**
 *
 * @author Joe
 */
public interface PropertyHolder {

    public Map<String, Property> getProperties();

    public boolean validatePropertyChange(Property property, Object value);

    public void propertyChanged(Property property);

}
