/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import React, { InputHTMLAttributes, Key, OptionHTMLAttributes } from "react";
import classNames from "classnames";
import { createVariantClass, Variant } from "../variants";
import { createAttributesForTesting } from "@scm-manager/ui-components";

type Props = {
  variant?: Variant;
  options?: Array<OptionHTMLAttributes<HTMLOptionElement> & { label: string }>;
  testId?: string;
} & InputHTMLAttributes<HTMLSelectElement>;

const Select = React.forwardRef<HTMLSelectElement, Props>(
  ({ variant, children, className, options, testId, ...props }, ref) => (
    <div className={classNames("select", { "is-multiple": props.multiple }, createVariantClass(variant), className)}>
      <select ref={ref} {...props} {...createAttributesForTesting(testId)} className={className}>
        {options
          ? options.map((option) => (
              <option {...option} key={option.value as Key}>
                {option.label}
                {option.children}
              </option>
            ))
          : children}
      </select>
    </div>
  )
);

export default Select;