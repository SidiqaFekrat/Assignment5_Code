package org.example.Amazon;

import org.example.Amazon.Cost.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AmazonIntegrationTest {
    private Database database;
    private ShoppingCart shoppingCart;
    private Amazon amazon;

    @BeforeEach
    void setUp() {
        database = new Database();
        database.resetDatabase();  // clean database before each test
        shoppingCart = new ShoppingCartAdaptor(database);

        // creating price rules
        List<PriceRule> rules = List.of(
                new RegularCost(),
                new DeliveryPrice(),
                new ExtraCostForElectronics()
        );

        amazon = new Amazon(shoppingCart, rules);
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }
    @Nested
    @DisplayName("specification-based")
    class SpecificationBasedTests {
        @Test
        @DisplayName("test to calculate regular item cost only, no delivery, no extra")
        void calculateRegularItemCost() {
            Item item = new Item(ItemType.OTHER, "Book", 2, 10.0);
            amazon.addToCart(item);
            double total = amazon.calculate();
            assertEquals(25.0, total );
        }

        @Test
        @DisplayName("test to calculate delivery price for 1-3 items + $5 for delivery")
        void calculateDeliveryForSmallOrder(){
            Item item1 = new Item(ItemType.OTHER, "Book1", 2, 40.0);
            Item item2 = new Item(ItemType.OTHER, "Book2", 1, 15.0);
            amazon.addToCart(item1);
            amazon.addToCart(item2);
            double total = amazon.calculate();
            assertEquals(2*40 + 15 +5, total);
        }
        @Test
        @DisplayName("test calculate delivery price for 4-10 items + 12.50 for delivery")
        void calculateDeliveryForMediumOrder(){
            for (int i = 0; i < 7; i++) {
                Item item = new Item(ItemType.OTHER, "Item" + i, 1, 35.0);
                amazon.addToCart(item);
            }
            double total = amazon.calculate();
            assertEquals(7 * 35 + 12.50, total);
        }
        @Test
        @DisplayName("test to calculate delivery price for 11+ items + $20 delivery fee")
        void calculateDeliveryForLargeOrder(){
            for (int i = 0; i < 13; i++) {
                Item item = new Item(ItemType.OTHER, "Item" + i, 1, 50.0);
                amazon.addToCart(item);
            }
            double total = amazon.calculate();
            assertEquals(13 * 50.0 + 20, total);
        }

        @Test
        @DisplayName("test for adding $7.50 extra for electronic items")
        void addExtraForElectronics(){
            Item electronic = new Item(ItemType.ELECTRONIC, "Laptop", 1, 800.0);
            Item regular = new Item(ItemType.OTHER, "Book", 1, 20.0);
            amazon.addToCart(electronic);
            amazon.addToCart(regular);
            double total = amazon.calculate();
            assertEquals((20 + 5) + (800+7.50 ), total);
        }
        @Test
        @DisplayName("test for calculate complex order (multiple items with mixed types)")
        void calculateComplexOrder(){
            for (int i = 0; i < 3; i++) {
                amazon.addToCart(new Item(ItemType.OTHER, "Book" + i, 1, 45.0));
            }
            for (int i = 0; i < 5; i++) {
                amazon.addToCart(new Item(ItemType.ELECTRONIC, "Device" + i, 1, 100.0));
            }
            double total = amazon.calculate();
            assertEquals( (3 * 45) + (5 *100) + (12.50 +7.50), total);
        }
        @Test
        @DisplayName("test for handling empty cart")
        void handleEmptyCard(){
            double total = amazon.calculate();
            assertEquals(0.0, total);
        }
        @Test
        @DisplayName("test to persist items across multiple calculate calls")
        void persistItemsInCart(){
            Item item = new Item(ItemType.OTHER, "Book", 1, 20.0);
            amazon.addToCart(item);
            double total1 = amazon.calculate();
            double total2 = amazon.calculate();
            assertEquals(25.0, total1);
            assertEquals(total1, total2);
        }
        @Test
        @DisplayName("test to add items with different quantities")
        void handleDifferentQuantities(){
            amazon.addToCart(new Item(ItemType.OTHER, "noteBook", 5, 10.0));
            amazon.addToCart(new Item(ItemType.OTHER, "pen", 3, 5.0));
            double total = amazon.calculate();
            assertEquals(5*10 + 3*5 + 5, total);
        }

    }

    @Nested
    @DisplayName("structured-based")
    class StructureBasedTests{
        @Test
        @DisplayName("test to retrieve items from database when calculating")
        void RetrieveItemsFromDatabase(){
            Item item = new Item(ItemType.OTHER, "Item1", 1, 50.0);
            amazon.addToCart(item);
            List<Item> items = shoppingCart.getItems();
            assertEquals(1, items.size());
            Item retrieved = items.get(0);
            assertEquals("Item1", retrieved.getName());
            assertEquals(50.0, retrieved.getPricePerUnit());
        }
        @Test
        @DisplayName("test verify all price rules are applied in sequence")
        void applyAllRulesInSequence() {
            amazon.addToCart(new Item(ItemType.ELECTRONIC, "Device", 1, 100.0));
            double total = amazon.calculate();
            assertEquals(1 *100 + 5+ 7.50 , total);
        }
        @Test
        @DisplayName("test to maintain cart state across add operations")
        void maintainCartState(){
            Item item1 = new Item(ItemType.OTHER, "Book", 1, 10.0);
            Item item2 = new Item(ItemType.OTHER, "Pen", 5, 2.0);
            amazon.addToCart(item1);
            List<Item> afterFirst = shoppingCart.getItems();
            amazon.addToCart(item2);
            List<Item> afterSecond = shoppingCart.getItems();
            assertEquals(1, afterFirst.size());
            assertEquals(2, afterSecond.size());
        }
        @Test
        @DisplayName("test to correctly calculate with varying ItemType values")
        void handleItemTypes(){
            Item regular = new Item(ItemType.OTHER, "Regular", 1, 50.0);
            Item electronic = new Item(ItemType.ELECTRONIC, "Electronic", 1, 100.0);
            amazon.addToCart(electronic);
            double withElectronic = amazon.calculate();
            database.resetDatabase();
            shoppingCart = new ShoppingCartAdaptor(database);
            amazon = new Amazon(shoppingCart, List.of(
                    new RegularCost(),
                    new DeliveryPrice(),
                    new ExtraCostForElectronics()
            ));
            amazon.addToCart(regular);
            double withoutElectronic = amazon.calculate();
            assertEquals( 1*100 + 5 + 7.50 , withElectronic);
            assertEquals(1*50 + 5, withoutElectronic);
            assertEquals(50+ 7.50, withElectronic - withoutElectronic, 0.01);

        }
        @Test
        @DisplayName("test to reset database between tests (verify via add/retrieve)")
        void resetDatabaseProperly(){
            Item item = new Item(ItemType.OTHER, "NewItem", 1, 5.0);
            amazon.addToCart(item);
            List<Item> items = shoppingCart.getItems();
            assertEquals(1, items.size());
            assertEquals("NewItem", items.get(0).getName());
        }
        @Test
        @DisplayName("test to handle all PriceRule implementations correctly")
        void applyAllRuleImplementations(){
            Item electronics = new Item(ItemType.ELECTRONIC, "Laptop", 1, 500.0);
            Item regular = new Item(ItemType.OTHER, "Mouse", 1, 25.0);
            amazon.addToCart(electronics);
            amazon.addToCart(regular);
            double total = amazon.calculate();
            assertEquals(500+25 + 5+ 7.50, total);
        }
    }

    }