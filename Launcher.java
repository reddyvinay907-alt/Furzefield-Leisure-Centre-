package leisure;

import leisure.bootstrap.DataBootstrap;
import leisure.console.MenuController;
import leisure.engine.BookingEngine;

/**
 * Entry point for the Furzefield Leisure Centre Booking System.
 *
 * Boot sequence:
 *   1. Instantiate BookingEngine
 *   2. Load sample data via DataBootstrap
 *   3. Launch the MenuController CLI
 */
public class Launcher {

    public static void main(String[] args) {
        BookingEngine engine = new BookingEngine();
        DataBootstrap.load(engine);

        MenuController menu = new MenuController(engine);
        menu.start();
    }
}
