package vallegrande.edu.pe.service;



import org.springframework.stereotype.Service;
import vallegrande.edu.pe.model.Product;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    public List<Product> getProducts() {

        List<Product> products = new ArrayList<>();

        products.add(new Product(1, "Laptop", 2500.00));
        products.add(new Product(2, "Mouse", 80.00));
        products.add(new Product(3, "Teclado", 120.00));
        products.add(new Product(4, "Monitor", 850.00));
        products.add(new Product(5, "Audífonos", 150.00));

        return products;
    }

    public String processProduct(String name) {

        String result = "";

        if (name != null) {

            if (name.length() > 0) {

                result = "Producto procesado: " + name;

            } else {

                result = "Producto vacío";

            }

        } else {

            result = "Producto no válido";
        }

        return result;
    }

    public boolean validateProduct(Product product) {

        if (product != null) {

            if (product.getName() != null) {

                if (!product.getName().isEmpty()) {

                    if (product.getPrice() != null) {

                        if (product.getPrice() > 0) {
                            return true;
                        }

                    }
                }
            }
        }

        return false;
    }
}