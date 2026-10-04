declare namespace Packages {
  namespace names {
    // omit this line if the class doesn't implement anything
    interface ExampleClass<T> extends ExampleInterface2<any> {}
    // add abstract modifier if it doesn't have any constructor
    class ExampleClass<T> extends java.lang.Object {
      // every java class/interface has this
      static readonly class: JavaClass<ExampleClass<any>>;
      // suppress this property because it's undefined in graaljs
      /** @deprecated */ static prototype: undefined;

      static readonly staticFinalField: 123;
      static staticFielda: number;
      static staticFieldb: string;
      // if you care about style, every multi line jsdoc should have an empty line here
      /**
       * description for this field
       */
      static staticFieldWithJsdoc: string;

      static staticMethoda(): void;
      static staticMethodb(): boolean;

      constructor();
      constructor(arg: any, arg2: boolean);

      readonly finalField: 456;
      fielda: number;
      fieldb: ExampleInterface2<any>;

      methoda(): void;
      methodb(): ExampleInterface<any>;
      // if you care about style, this gap should be removed if the last element is field
    }

    // should always be abstract and extends java.lang.Interface
    abstract class ExampleInterface<T> extends java.lang.Interface {
      static readonly class: JavaClass<ExampleInterface<any>>;
      /** @deprecated */ static prototype: undefined;

      static readonly staticFinalField: 123;
      static staticFielda: number;
      static staticFieldb: string;

      static staticMethoda(): void;
      static staticMethodb(): boolean;
      // this gap, same as the class above
    }
    interface ExampleInterface<T> extends ExampleInterface2<any> {
      readonly finalField: 456;
      fielda: ExampleInterface2<any>;
      fieldb: string;

      methoda(): void;
      methodb(i: ExampleInterface2<any>): object;
      // this gap, same as the class above
    }

    abstract class ExampleInterface2<T> extends java.lang.Interface {
      static readonly class: JavaClass<ExampleInterface2<any>>;
      /** @deprecated */ static prototype: undefined;
    }
    interface ExampleInterface2<T> {}

    // if there's conflicting keyword, resolve it by exporting them
    namespace _function {}

    // exporting this way instead of adding `export` keyword before classes is important for docs readability, if you don't do this the types will be long asf
    export {
      ExampleClass,
      ExampleInterface,
      ExampleInterface2,
      _function as function
    };
  }
  // split namespace (`names` for this example) for exports and namespaces that doesn't need export
  namespace names {

    namespace subnamespace {}

    namespace subnamespace2 {}
  }
}

// ignore this, it's just preventing the classes in this example from exporting
export {};
